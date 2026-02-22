// Class to store song details (i.e Title, Artist, length)
package com.musicplayer;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileReader;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.TitlePaneLayout;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.mpatric.mp3agic.Mp3File;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

public class Song {

    private String songTitle;
    private String songArtist;
    private String formatedSonglength;
    private long songLengthInMilli;
    private String filepath;
    private String extension;
    private Mp3File mp3File;
    private double frameRatePerMilliSeconds;
    private long songFrameCount;

    public Song(String filePath){
        this.filepath = filePath;

        this.extension = getFileExtension(new File(filePath)).toUpperCase();

        this.songArtist = "Unknown";
        this.songTitle = "N/A";

        if(extension.contains("MP3")){
            readFileMP3(filePath);
        }else{
            readFileWav(new File(filePath));
            mp3File = null;
        }

    }

    private String getFileExtension(File songfile){
        int lastDot = songfile.getName().lastIndexOf(".");
         return lastDot == -1 ? "" : songfile.getName().substring(lastDot + 1);
    }

    public void readFileMP3(String filePath){
        try{
            mp3File = new Mp3File(filePath);
            this.frameRatePerMilliSeconds = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds();
            
            // format the song into mm:ss
            formatedSonglength = formatSongLengthMP3();
                
            // Create an audio file
            AudioFile audioFile = AudioFileIO.read(new File(this.filepath));

            // Retrieve the length of the song
            this.songLengthInMilli = mp3File.getLengthInMilliseconds();

            // Maximum frame count
            this.songFrameCount = mp3File.getFrameCount();
                
            // Extract data through the audio files tag
            extractArtistAndTitleMP3(audioFile);

        }catch(Exception e){
             e.printStackTrace();
        }        
    }

    public void readFileWav(File songFile){
        try{
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(songFile);
            this.songFrameCount = audioInputStream.getFrameLength();
            float frameRate = audioInputStream.getFormat().getFrameRate();

            this.frameRatePerMilliSeconds = frameRate / 1000.0;

            // Duration in milliseconds
            this.songLengthInMilli = (long)(songFrameCount / frameRate * 1000);

            // Formatted song length - String
            this.formatedSonglength = formatSongLengthWav();
                
            // Extract data through the audio files tag
            extractArtistAndTitleWav(songFile);

        }catch(Exception e){
            e.printStackTrace();
        }  
    }

    public void extractArtistAndTitleMP3(AudioFile audioFile){
        
        // Extract data through the audio files tag if MP3
        Tag tag = audioFile.getTag();
            
        System.out.println(tag.getFirst(FieldKey.TITLE).isEmpty());

        if (tag != null){
            this.songTitle = tag.getFirst(FieldKey.TITLE);
            this.songArtist = tag.getFirst(FieldKey.ARTIST);

            if(tag.getFirst(FieldKey.TITLE).isEmpty()){
                ArtistAndTitleFromName(new File(this.filepath));
            }
        }
    } 

    public void extractArtistAndTitleWav(File file){
          
        // Try reading metadata using Apache Tika
        try{
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(file.toURI().toURL().openStream(), new BodyContentHandler(), metadata);

            this.songTitle = metadata.get("title");
            this.songArtist = metadata.get("xmpDM:artist");

            // Fallback to filename parsing
            if (songTitle == null || songTitle.isEmpty()) {
                ArtistAndTitleFromName(file);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void ArtistAndTitleFromName(File file){

        String name = file.getName();

        if (name.contains("-")) {
            String[] parts = name.split("-");
            this.songTitle = parts[0].trim();
            this.songArtist = parts[1].replaceFirst("\\.[^.]+$", "").trim(); // remove extension
        } else {
            this.songArtist = "Unknown";
            this.songTitle = name.replaceFirst("\\.[^.]+$", "");
        }
    }

    public String formatSongLengthMP3(){
        long minutes = mp3File.getLengthInSeconds() / 60;
        long seconds = mp3File.getLengthInSeconds() % 60;

        String lengthFormat = String.format("%02d:%02d", minutes, seconds);

        return lengthFormat;
    }

    public String formatSongLengthWav(){
        long minutes = (songLengthInMilli/1000) / 60;
        long seconds = (songLengthInMilli/1000) % 60;

        String lengthFormat = String.format("%02d:%02d", minutes, seconds);

        return lengthFormat;      
    }

    public String getSongTitle(){
        return this.songTitle;
    }

    public String getSongArtists(){
        return this.songArtist;
    }

    public String getFormatedsongLength(){
        return this.formatedSonglength;
    }

        public Long getSongLengthInMilli(){
        return this.songLengthInMilli;
    }

    public String getFilePath(){
        return this.filepath;
    }

    public Mp3File getMp3File(){
        return this.mp3File;
    }

    public String getSongExtension(){
        return this.extension;
    }

    public long getMaximumFrameCount(){
        return this.songFrameCount;
    }

    public double getFrameRatePerMilliSeconds(){
        return this.frameRatePerMilliSeconds;
    }
}
