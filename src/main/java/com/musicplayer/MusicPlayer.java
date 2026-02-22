package com.musicplayer;

import javazoom.jl.player.advanced.PlaybackListener;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.SwingUtilities;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;

public class MusicPlayer extends PlaybackListener{
    // this will be used to update is paused more synchronously
    protected static final Object playSignal = new Object();

    // reference of the GUI class so we can update the GUI from this class
    protected MusicPlayerGUI musicPlayerGUI;

    // Storing song details for the current song
    protected Song currentSong;

    // Storing the current song index
    protected int currentSongIndex;

    // The total time of the song
    protected long songTimeInMilli;

    // Storing the songs from the playlist
    protected ArrayList<Song> playList;

    // Use JLayer Library to create an AdvancedPlayer obj
    private AdvancedPlayer advancedPlayer;

    // Pause boolean flag used to indicate if Music player is paused
    protected boolean isPaused;

    protected boolean hasEnded;

    // Busy boolean flag used to indicate if Music player has a song
    protected boolean isBusy = false;

    // The time the song was played for (mili-seconds)
    protected int currentFrame;

    // Keep update of the time passed since the song has started
    protected int currentTimeInMilli;

    // Desired volume in decibles
    protected float currentVolumeInDecibels;

    // Prior volume in decibles
    protected float previousVolumeInDecibels;

    // Java device associated with the GUI
    private JavaSoundAudioDevice activeDevice;

    public MusicPlayer(MusicPlayerGUI musicPlayerGUI){
        this.musicPlayerGUI = musicPlayerGUI;
        this.playList = new ArrayList<>();
    }

    public boolean getPlayerIsPaused(){
        return this.isPaused;
    }

    public boolean getPlayerHasEnded(){
        return this.hasEnded;
    }

    public boolean getPlayerIsBusy(){
        return this.isBusy;
    }

    public Song getCurrentSong(){
        return this.currentSong;
    }

    public float getMusicPlayerVolume(){
        return this.currentVolumeInDecibels;
    }

    public void setCurrentFrame(int frame){
        this.currentFrame = frame;
    }

    public void setCurrentTimeInMilli(int timeInMilli){
        this.currentTimeInMilli = timeInMilli;
    }

    private void setSongTimeInMilli(Song song){
        songTimeInMilli = song.getSongLengthInMilli();
    }

    public void setMusicPlayerVolume(float volume){
        previousVolumeInDecibels = currentVolumeInDecibels;
        currentVolumeInDecibels = volume;
    }

    public void loadSong(Song song){
        // If previous playlist remains
        if(!playList.isEmpty()){
            playList.clear();
        }

        // Update the current song
        currentSong = song;

        setSongTimeInMilli(currentSong);

        // Reset playback state
        currentFrame = 0;
        currentTimeInMilli = 0;

        // Play current song if not null
        if(currentSong != null){
            playCurrentSong();
        }
    }

    public void loadPlayList(File playListFile){

        try(BufferedReader bufferReader = new BufferedReader(new FileReader(playListFile))) {
           
            String path;
            while ((path = bufferReader.readLine()) != null) {
                path = path.trim();

                if (!path.isEmpty()) {
                    Song song = new Song(path);
                    playList.add(song);
                }
            }
        } 
        catch (Exception e){
            e.printStackTrace();
        }

        if (!playList.isEmpty()) {
            // Reset playback slider
            musicPlayerGUI.setPlayBackSliderValue(0);
            currentTimeInMilli = 0;

            // Update current song
            currentSong = playList.get(0);

            // Start from begining frame
            currentFrame = 0;

            musicPlayerGUI.disablePlayButtonAndEnablePauseButton();
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);
            musicPlayerGUI.updatePlayBackSlider(currentSong);
            
            setSongTimeInMilli(currentSong);
            playCurrentSong();
        }
    }

    protected void changeMusicPlayerVolume(float dB, JavaSoundAudioDevice device) {
        try {
            // We use Reflection to get the 'line' because it's protected in JLayer
            java.lang.reflect.Field field = JavaSoundAudioDevice.class.getDeclaredField("source");
            field.setAccessible(true);
            SourceDataLine line = (SourceDataLine) field.get(device);

            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                // Volume smoothing
                float step = (dB - previousVolumeInDecibels) / 3;

                volumeControl.setValue(previousVolumeInDecibels + step);
                volumeControl.setValue(previousVolumeInDecibels + (step * 2));
                volumeControl.setValue(dB);
            }
        } catch (Exception e) {
            System.err.println("Error: Volume control error -> " + e.getMessage());
        }
    }

    public void playCurrentSong(){
        // read MP3s audio data
        try{
            FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            // Create the AudioDevice manually so we can control it
            activeDevice = new JavaSoundAudioDevice() {
                @Override
                public void write(short[] samples, int offs, int len) {
                    // Initiate the sound first
                    try {
                        super.write(samples, offs, len);
                    } catch (JavaLayerException e) {
                        e.printStackTrace();
                    }

                    // Calculate the intensity using rms method
                    float rawIntensity = calculateRMS(samples);

                    float sensitiveIntensity = (float) Math.pow(rawIntensity * 25.0f, 0.7);

                    float boosted = Math.max(0.0f, Math.min(1.0f, sensitiveIntensity));

                    SwingUtilities.invokeLater(() -> {
                        if (musicPlayerGUI != null && musicPlayerGUI.lightingPanel != null) {
                            musicPlayerGUI.lightingPanel.setIntensity(boosted);
                        }
                    });
                }
            };

            // Create a new advanced player
            advancedPlayer = new AdvancedPlayer(bufferedInputStream, activeDevice);
            advancedPlayer.setPlayBackListener(this);

            // Start music
            startMusicThread();

            // Create a small delay (100ms) to ensure the audio line exists
            new Thread(() -> {
                try {
                    Thread.sleep(100); 
                    changeMusicPlayerVolume(currentVolumeInDecibels, activeDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Update the is busy flag
            isBusy = true;

            // Start to update the sider
            startPlayBackSliderThread();

        }catch(Exception e){
            System.out.println("Error in starting player");
            e.printStackTrace();
        }
    }

    // Pause the music player
    public void pauseSong(){
        if(advancedPlayer != null){

            // Update the flag
            isPaused = true;

            // Stop the music player
            stopSong();

            // Update the player has ended flag
            hasEnded = false;
        }
    }

    public void stopSong(){
        if(advancedPlayer != null){
            // Stop the music player
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;

            // Update the is busy and player has ended flag
            isBusy = false;
            hasEnded = true;
        }
    }

    public void nextSong(){
        // Check if the playlist is populated
        if(playList.isEmpty()) return;

        // Stop the player if there is a song playing
        if(!isPaused && !hasEnded){
            stopSong();
        }
  
        // Play the next song if there is a in the songlist
        if (currentSongIndex + 1 < playList.size()) {
            // Increment the song
            currentSongIndex++;
        } else{
            // Move back to the first song
            currentSongIndex = 0;
        }

        // Update current song to the next logical song
        currentSong = playList.get(currentSongIndex);

        // Reset playback slider
        musicPlayerGUI.setPlayBackSliderValue(0);
        currentTimeInMilli = 0;
        
        // Start from begining frame
        currentFrame = 0;

        musicPlayerGUI.disablePlayButtonAndEnablePauseButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlayBackSlider(currentSong);

        // Play the song
        playCurrentSong();
    }

    public void previousSong(){
        // Check if the playlist is populated
        if(playList.isEmpty()) return;

        // Stop the player if there is a song playing
        System.out.println("Previous button internal pause: " + isPaused);
        if(!isPaused && !hasEnded){
            stopSong();
        }

        // Play the next song if there is a in the songlist
        if (currentSongIndex + 1 <= playList.size() && currentSongIndex != 0) {
            // Decrement the song
            currentSongIndex--;
        } else{
            // Wrap to last
            currentSongIndex = playList.size() - 1;
        }

        // Update current song to the next logical song
        currentSong = playList.get(currentSongIndex);

        // Reset playback slider
        musicPlayerGUI.setPlayBackSliderValue(0);
        currentTimeInMilli = 0;
        
        // Start from begining frame
        currentFrame = 0;

        musicPlayerGUI.disablePlayButtonAndEnablePauseButton();
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);
        musicPlayerGUI.updatePlayBackSlider(currentSong);

        // Play the song
        playCurrentSong();
    }

    // Create a thread that will handle playing the music
    protected void startMusicThread(){
        new Thread(new Runnable() {
            @Override
            public void run(){
                try{
                    if(isPaused){
                        
                        synchronized(playSignal){
                            // Update is pause flag
                            isPaused = false;
                            hasEnded = false;

                            // Notify the otherthreads to continue
                            playSignal.notify();

                        }

                        // Resume from the paused instance
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);

                    }else{
                         // Play music from the start
                        advancedPlayer.play();
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Create a thread that will handle updating a slider
    public void startPlayBackSliderThread(){
        new Thread(new Runnable(){
            @Override
            public void run(){

                if(isPaused){
                    try{

                        synchronized(playSignal){
                            playSignal.wait();
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
                
                try {
                    while(!isPaused){
                        // Increment the current time while the song is being played
                        currentTimeInMilli++;

                        // Convert the time in mili into fram
                       int calculateFrame = (int) ((double) currentTimeInMilli * 2.08 * currentSong.getFrameRatePerMilliSeconds());

                        // Update the slider in the GUI
                        musicPlayerGUI.setPlayBackSliderValue(calculateFrame);

                        // mimic 1 miliseconds using thread.sleep
                        Thread.sleep(1);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        isPaused = false;
        hasEnded = false;

        System.out.println("Is Paused: " + isPaused);
        
        // Called at the start of the Song
        System.out.println("Playback started \n");

    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {


        System.out.println(currentSong.getSongLengthInMilli());
        System.out.println((int) (currentTimeInMilli*2.08));     
        System.out.println('\n');
        
        if((int) (currentTimeInMilli*2.08) >= songTimeInMilli){
            // Update the value of is paused and has ended
            hasEnded = true;
            isPaused = true;
        }

        System.out.println("Is Paused: " + isPaused);
        System.out.println("Has Ended: " + hasEnded);

        // Get the finished time stamp from the event -> Only if paused [Play next if true]
        if (!hasEnded){
            System.out.println("Playback Paused \n");
            currentFrame += (int) ((double) evt.getFrame() * currentSong.getFrameRatePerMilliSeconds());
        }else{
            // When the song ends and there is no playlist
            // Called at the end of the song or When the pLayer is stopped
            System.out.println("Playback finished \n");

            if(playList.isEmpty()){
                // Update GUI
                musicPlayerGUI.disablePauseButtonAndEnablePlayButton();
            }
            // If there is a playlist play the next song
            else{
                if(currentSongIndex == playList.size()-1){
                    // Update GUI
                    musicPlayerGUI.disablePauseButtonAndEnablePlayButton(); 
                }else{
                    // go to the next song
                    nextSong();
                }
            }
        } 
    }    

    private float calculateRMS(short[] samples) {
        double sum = 0;
        
        for (short sample : samples) {
            sum += sample * sample;
        }

        double rms = Math.sqrt(sum / samples.length);

        return (float) (rms / 32768.0);
    }

}
