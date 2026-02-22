package com.musicplayer;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.ceau.itunesapi.response.Response;
import be.ceau.itunesapi.response.Result;
import be.ceau.itunesapi.Search;
import be.ceau.itunesapi.request.Entity;
import be.ceau.itunesapi.request.search.Media;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MusicDataService {

    private Map<String, String> songDataMap = new HashMap<>();

    @Before
	public void setup() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
	}

    // The brains behind the data service
    public Map<String, String> getCompleteMetadata(String title, String artist) {
        
        Map<String, String> database = null;

        // Check if there is both title and artists are available
        if (title.equals("N/A") || artist.equals("Unknown")) {
            return applyGenericTheme(title, artist);
        }

        // Get data from your Python API
        Map<String, String> localData = fetchFromPythonAPI(title, artist);

        if (localData != null) {
            
            System.out.println("Match found in personal database!");
            database = localData;
            
        }else{
            
            // Look up data from Apple library using ITunes API
            System.out.println("\nNot in local DB. Searching ITunes ...\n");
            
            Map<String, String> remoteData = fetchFromITunes(title, artist);

            if(remoteData != null){
                database = remoteData;

                // Update the local database
                saveToPersonalDatabase(remoteData);
            }

        }

        return database;
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
        var resource = getClass().getResource("/record.png");
        songDataMap.put("artwork", resource.toString());
        return songDataMap;
    }

    // Function to retrieve data from the personal database - API service in python
    private Map<String, String> fetchFromPythonAPI(String title, String artist) {

        // Prepare the URL for only GET:Title search
        String urlTitle = String.format("http://127.0.0.1:8000/songs/%s",
                URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"));

        HttpResponse<String> response = executeHttpResponse(urlTitle);

        // Extract the GET response
        if (response != null && response.statusCode() == 200) {
            return parsePythonJsonToMap(response.body());
        }else{

            // Prepare the URL for only GET:Title+Artist search
            String urlFullSearch = String.format("http://127.0.0.1:8000/songs/%s/%s",
                    URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20"),
                    URLEncoder.encode(artist, StandardCharsets.UTF_8).replace("+", "%20"));
            
            HttpResponse<String> responseFullSearch = executeHttpResponse(urlFullSearch);

            if (responseFullSearch != null && responseFullSearch.statusCode() == 200) {
                return parsePythonJsonToMap(responseFullSearch.body());
            }else{
                return null;
            }
        }
    }

    // Execute the http response
    private HttpResponse<String> executeHttpResponse(String url){
        
        // Create the Client and Request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        
        System.out.println(request);

        try{
            // Execute the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response;

        }catch(Exception e) {
            System.err.println("Error connecting to Python API: " + e.getMessage());

            return null;
        }

    }

    // Function to retrieve data from the ITunes API
    private Map<String, String> fetchFromITunes(String title, String artist){
        // Clear old data so we don't have leftovers
        this.songDataMap.clear();

        String combinedQuery = URLEncoding(title, artist);

        try{
            Response response = new Search()
            .setTerm(combinedQuery)
            .setEntity(Entity.SONG)
            .setMedia(Media.MUSIC)
            .setLimit(10)
            .execute();

            List<Result> results = response.getResults();
            
            if (results != null && !results.isEmpty()) {
                
                Result result = results.get(0);
                
                // Extract the data set
                songDataMap.put("artist", result.getArtistName());                    
                songDataMap.put("title", result.getTrackName());
                songDataMap.put("album", result.getCollectionName());
                songDataMap.put("date of release", result.getReleaseDate());
                songDataMap.put("genre", result.getPrimaryGenreName());
                songDataMap.put("artwork", result.getArtworkUrl100());

                return songDataMap;
            }else{
                System.out.println("Search returned no results for: " + combinedQuery + " in the ITUNES Library. \n");

                return applyGenericTheme(title, artist);
            }
        }catch(Exception e){
            System.err.println("ITunes lookup failed: " + e.getMessage());
        }
        
        return applyGenericTheme(title, artist);
    }

    private String URLEncoding(String title, String artist) {
        // Remove values in brackets
        String cleanTitle = title.replaceAll("\\s*\\(.*?\\)", "").trim();
        String cleanArtist = artist.replaceAll("\\s*\\(.*?\\)", "").trim();

        // Remove any special characters
        cleanTitle = cleanTitle.replaceAll("[^a-zA-Z0-9 ]", "");
        cleanArtist = cleanArtist.replaceAll("[^a-zA-Z0-9 ]", "");

        // Combine the title and artist
        String combinedQuery = cleanTitle + " " + cleanArtist;
        
        return combinedQuery;
    }

    // Update the local API with data found from iTunes
    private void saveToPersonalDatabase(Map<String, String> remoteData) {
        try {
            // Convert the Map to a JSON string
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(remoteData);

            // The HTTP POST request
            //String url = String.format("http://127.0.0.1:8000/songs/%s/%s",
            //        URLEncoder.encode(remoteData.get("title"), StandardCharsets.UTF_8).replace("+", "%20"),
            //        URLEncoder.encode(remoteData.get("artist"), StandardCharsets.UTF_8).replace("+", "%20"));

            String url = String.format("http://127.0.0.1:8000/songs");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Asyncronious data transfer
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        System.out.println("Successfully cached to local database.");
                    } else {
                        System.err.println("Python API rejected save. Status: " + response.statusCode());
                    }
                });

        } catch (Exception e) {
            System.err.println("Failed to save to personal database: " + e.getMessage());
        }
    }

    // Parse the Python Json response to a map
    private Map<String, String> parsePythonJsonToMap(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            Map<String, Object> result = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                
            // Update your class property
            this.songDataMap.clear();
            
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                this.songDataMap.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            return this.songDataMap;
        } catch (Exception e) {
            System.err.println("JSON Parsing Error: " + e.getMessage());
            return null; 
        }
    }

}
