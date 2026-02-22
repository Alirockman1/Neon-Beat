package com.musicplayer;

import static com.musicplayer.MusicPlayer.playSignal;

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.PlaybackEvent;

public class MusicPlayerWAV extends MusicPlayer{

    private Clip clip;

    public MusicPlayerWAV(MusicPlayerGUI musicPlayerGUI){
        super(musicPlayerGUI);
    }

    @Override
    protected void changeMusicPlayerVolume(float dB, JavaSoundAudioDevice device) {
        try {
            // We use the min and max setting of the clip
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                
                // Constraints check: MASTER_GAIN usually ranges from -80.0 to 6.0
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                if (dB < min) dB = min;
                if (dB > max) dB = max;

                // Volume smoothing
                float step = (dB - previousVolumeInDecibels) / 3;

                volumeControl.setValue(previousVolumeInDecibels + step);
                volumeControl.setValue(previousVolumeInDecibels + (step * 2));
                volumeControl.setValue(dB);
            }
        } catch (Exception e) {
            System.err.println("Could not set volume: " + e.getMessage());
        }
    }

    @Override
    public void playCurrentSong(){
        // read WAVs audio data
        try{
            // Open the audio file
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(super.currentSong.getFilePath()));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            
            // Get a clip resource
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Update the is busy flag
            super.isBusy = true;

            // Start music
            startMusicThread();

            // Create a small delay (100ms) to ensure the audio line exists
            new Thread(() -> {
                try {
                    Thread.sleep(100); 
                    changeMusicPlayerVolume(currentVolumeInDecibels, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Start to update the sider
            startPlayBackSliderThread();
        }catch(Exception e){
            System.out.println("Error in starting player");
            e.printStackTrace();
        }
    }

    // Start playback from scratch
    public void play() {
        if (clip != null) {
            clip.start();
        }
    }

    // Start playback after pause
    public void play(int currentFrame, int max_value) {
        if (clip != null) {
            clip.setFramePosition(currentFrame);
            clip.start();
        }
    }

    // Pause the music player
    @Override
    public void pauseSong(){
        if(clip != null){

            // Update the flag
            isPaused = true;

            // Stop the music player
            stopSong();

            // Update the player has ended flag
            hasEnded = false;
        }
    }

    // Stop playback
    @Override
    public void stopSong(){
        if(clip != null){
            // Stop the music player
            clip.stop();
            clip.close();
            clip = null;

            // Update the is busy and player has ended flag
            isBusy = false;
            hasEnded = true;
        }
    }

    // Create a thread that will handle playing the music
    @Override
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
                        play(currentFrame, Integer.MAX_VALUE);

                    }else{
                         // Play music from the start
                        play();
                    }
                }catch(Exception e){
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

            System.out.println(playList.isEmpty());

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

}
