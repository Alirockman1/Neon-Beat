package com.musicplayer.Backup;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.musicbrainz.MBWS2Exception;
import org.musicbrainz.controller.Recording;
import org.musicbrainz.model.ArtistCreditWs2;
import org.musicbrainz.model.TagWs2;
import org.musicbrainz.model.searchresult.RecordingResultWs2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.musicbrainz.model.entity.RecordingWs2;
import org.musicbrainz.model.entity.ReleaseWs2;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MusicDataService {

    private Map<String, String> songDataMap = new HashMap<>();

    // The brains behind the data service
    public Map<String, String> getCompleteMetadata(String title, String artist) {
        Logger.getLogger("org.musicbrainz").setLevel(Level.OFF);
        Logger.getLogger("org.apache.http").setLevel(Level.OFF);
        
        // Check if there if both title and artists are available
        if (title.equals("N/A") || artist.equals("Unknown")) {
            return applyGenericTheme(title, artist);
        }else{
            
        }
        // Get data from your Python API
        Map<String, String> localData = fetchFromPythonAPI(title, artist);
        
        if (localData != null) {
            System.out.println("Match found in personal database!");
            return localData;
        }

        // Look up data from MusicBrainz
        System.out.println("Not in local DB. Searching MusicBrainz...");
        Map<String, String> remoteData = fetchFromMusicBrainz(title, artist);

        System.out.println(remoteData);

        // If we found it on MusicBrainz, save it to the Python API for next time
        //if (remoteData != null) {
        //    saveToPersonalDatabase(remoteData);
        //}

        return remoteData;
    }

    // Function to return default data
    private Map<String, String> applyGenericTheme(String title, String artist) {
        // Clear old data so we don't have leftovers
        songDataMap.clear();
        
        songDataMap.put("title", title);
        songDataMap.put("artist", artist);
        songDataMap.put("album", "Unknown");
        songDataMap.put("genre", "Unknown");
        songDataMap.put("date of release", "N/A");

        // Use the file protocol for local images
        File fallbackAssetFile = new File("assets/record.png");
        songDataMap.put("artwork", fallbackAssetFile.toURI().toString()); //To Do: change to relative path
        return songDataMap;
    }

    // Function to retrieve data from the personal database - API service in python
    private Map<String, String> fetchFromPythonAPI(String title, String artist) {
        try {
            // 1. Prepare the URL (Encoding handles spaces/special chars in song names)
            String url = String.format("http://localhost:5000/search?title=%s&artist=%s",
                    URLEncoder.encode(title, StandardCharsets.UTF_8),
                    URLEncoder.encode(artist, StandardCharsets.UTF_8));

            // 2. Create the Client and Request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // 3. Execute the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Check if the Python API actually found the song (Status 200)
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                return parsePythonJsonToMap(jsonResponse);
            }

        } catch (Exception e) {
            System.err.println("Error connecting to Python API: " + e.getMessage());
        }

        return null; // Triggers the fallback to MusicBrainz in your 'brains' function
}

    // Function to retrieve data from the MusicBrains API
    private Map<String, String> fetchFromMusicBrainz(String title, String artist){
        // Clear old data so we don't have leftovers
        this.songDataMap.clear();

        try {

            Thread.sleep(1100);
        
            // 1. Setup the Controller
            Recording recordingController = new Recording();
            recordingController.getSearchFilter().setLimit(1L); 

            // Search for the song by title and artist
            String query = buildMusicBrainzQuery(title, artist);
            System.out.println(query);
            recordingController.search(query);

            // Retrieve entire database
            List<RecordingResultWs2> results = recordingController.getFullSearchResultList();
            System.out.println("Here I am");
            System.out.println(results);

            if (results != null && !results.isEmpty()) {
                RecordingResultWs2 bestMatch = results.get(0);
                RecordingWs2 recording = bestMatch.getRecording();

                songDataMap.put("artist", artist);
                    
                songDataMap.put("title", recording.getTitle());

                if (recording.getReleases() != null && !recording.getReleases().isEmpty()) {
                    // We take the first release found for this recording
                    ReleaseWs2 release = recording.getReleases().get(0);
                        
                    songDataMap.put("album", release.getTitle());
                    songDataMap.put("date of release", release.getDate().toString());
                        
                    // 3. ARTWORK (Based on the Album ID)
                    String releaseMbid = release.getId();
                    songDataMap.put("artwork", "https://coverartarchive.org/release/" + releaseMbid + "/front");
                    }

                if (recording.getTags() != null && !recording.getTags().isEmpty()) {
                    songDataMap.put("genre", recording.getTags().get(0).getName());
                } else {
                    songDataMap.put("genre", "Unknown");
                }

                return songDataMap;
                }
        } catch (Exception e) {
            System.out.println("MusicBrainz lookup failed: " + e.getMessage());
        }

        return applyGenericTheme(title, artist);
    }

    private String buildMusicBrainzQuery(String title, String artist) {
        // 1. Remove special characters that break Lucene (like quotes or brackets in the title)
        String cleanTitle = title.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        
        StringBuilder query = new StringBuilder();
        // Wrap title in quotes so it's a phrase search
        query.append("recording:\"").append(cleanTitle).append("\"");

        // 2. Split the artist string and take the first name for broad matching
        String[] individualArtists = artist.split("&|,|feat\\.");
        for (String name : individualArtists) {
            String cleanName = name.trim().replaceAll("[^a-zA-Z0-9 ]", "");
            if (!cleanName.isEmpty()) {
                // Add as a keyword, not a phrase, to be more flexible
                query.append(" AND artist:").append(cleanName.split(" ")[0]);
            }
        }
        return query.toString();
    }

    // Parse the Python Json response
    private Map<String, String> parsePythonJsonToMap(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            Map<String, String> result = mapper.readValue(json, new TypeReference<Map<String, String>>() {});
                
            // Update your class property
            this.songDataMap.clear();
            this.songDataMap.putAll(result);
            
            return this.songDataMap;
        } catch (Exception e) {
            System.err.println("JSON Parsing Error: " + e.getMessage());
            return null; 
        }
    }

    // Update the local API
    private void saveToPersonalDatabase(Map<String, String> remoteData){

    }
}
