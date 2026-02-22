package com.musicplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.StyledEditorKit.BoldAction;

public class MusicPlayerGUI extends JFrame {

    public static final Color BACKGROUND_COLOR = Color.BLACK, TEXT_COLOR = Color.WHITE, FRAME_COLOR = Color.DARK_GRAY;

    private String fileDirectory = "assets";
    private MusicPlayer musicPlayer;
    private JFileChooser fileChooser = new JFileChooser();
    private Song newSong; 
    private JLabel songTitle, songArtist, songImage;
    private JPanel playbackButtons;
    private JSlider playBackSlider;
    private JSlider volumeSlider;
    private float volumeInDecibels;
    private MusicDataService musicDataService;

    private int imageHeight, imageWidth;

    public LightingPanel lightingPanel;

    public MusicPlayerGUI(){
        
        // Set title of the frame
        super("Music Player");

        // define the frame size
        setSize(400, 575);

        // End process as soon as the app is closed
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Launch the app on the center
        setLocationRelativeTo(null);

        // Disable resizing
        setResizable(false);

        setLayout(null);

        // Set a default path for file explorer
        fileChooser.setCurrentDirectory(new File("assets"));
        
        // Filter file selecter to only (Audio) File format
        // To Do: Add WAV and WMA (windows) //     
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV", "wav"));
        
        // MP3 file format
        fileChooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));
        
        // Set the color of the player frameS
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Create the music player instance
        this.musicPlayer = null;

        // Initialize the music player volume
        this.volumeInDecibels = 0.0f;

        // Initialize the MusicDataService
        this.musicDataService = new MusicDataService();
 
        addGuiComponents();

    }

    public String getFileDirectory(){
        return this.fileDirectory;
    }

    private String getFileExtension(File songfile){
        int lastDot = songfile.getName().lastIndexOf(".");
         return lastDot == -1 ? "" : songfile.getName().substring(lastDot + 1);

    }

    private void addGuiComponents(){
        // Add toolbar
        addToolbar();

        // Load record icon
        this.songImage = new JLabel(loadImage("/record.png"));
        songImage.setBounds(0, 55, getWidth()-20, 225);
        this.imageHeight = songImage.getHeight();
        this.imageWidth = 205;
        add(songImage);

        // Add the middle layer for the GUi
        this.lightingPanel = new LightingPanel();
        lightingPanel.setBounds(42, 20, 295, 295);
        add(this.lightingPanel);

        // Song title
        this.songTitle = new JLabel("Song Title");
        songTitle.setBounds(0, 310, getWidth()-10, 30);
        songTitle.setFont(new Font("Dialog", Font.BOLD, 24));
        songTitle.setForeground(TEXT_COLOR);
        songTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(songTitle);

        // Song Artist
        this.songArtist = new JLabel("Artist");
        songArtist.setBounds(0, 345, getWidth()-10, 30);
        songArtist.setFont(new Font("Dialog", Font.PLAIN, 24));
        songArtist.setForeground(TEXT_COLOR);
        songArtist.setHorizontalAlignment(SwingConstants.CENTER);
        add(songArtist);

        // Playback slider
        playBackSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        playBackSlider.setBounds((getWidth()-300)/2, 390, 300, 40);
        playBackSlider.setForeground(BACKGROUND_COLOR);
        playBackSlider.setBackground(null);

        playBackSlider.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                // Pause the song when the slider tick is in process of being moved
                if(!musicPlayer.getPlayerIsPaused()){
                    musicPlayer.pauseSong();
                }

                // Toggle on the pause button and off the play button
                disablePlayButtonAndEnablePauseButton();
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                if (newSong != null){

                    // When the mouse is released we want to record the new posiion
                    JSlider source = (JSlider) e.getSource();

                    // Get the frame value
                    int sliderFrame = source.getValue();

                    // Update the frame in the music player
                    musicPlayer.setCurrentFrame(sliderFrame);

                    // Update the time in miliseconds
                    int sliderTimeInMilli = (int) (sliderFrame / (2.08 * musicPlayer.getCurrentSong().getFrameRatePerMilliSeconds()));
                    musicPlayer.setCurrentTimeInMilli(sliderTimeInMilli);

                    // Resume the current song
                    musicPlayer.playCurrentSong();

                    // Toggle on the play button and off the pause button
                    disablePlayButtonAndEnablePauseButton();
                }
            }
        });
        add(playBackSlider);

        // Volume vertical slider
        volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
        volumeSlider.setBounds(330, (getHeight()-350)/2, 40, 100);
        volumeSlider.setForeground(BACKGROUND_COLOR);
        volumeSlider.setBackground(null);
        volumeSlider.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                // Pause the song when the slider tick is in process of being moved
                if(!musicPlayer.getPlayerIsPaused() && musicPlayer.getCurrentSong() != null){
                    musicPlayer.pauseSong();
                }

                // Toggle on the pause button and off the play button
                disablePlayButtonAndEnablePauseButton();
            }

            @Override
            public void mouseReleased(MouseEvent e){
                
                // When the mouse is released we want to record the new posiion
                JSlider source = (JSlider) e.getSource();

                // 1. Get the value of the slider
                int sliderPosition = source.getValue();

                float sliderValue = sliderPosition / 100.0f;

                // 2. Square the value to decrease the volume down faster.
                float squaredValue = sliderValue * sliderValue;

                // 3. Convert to Decibels
                if (sliderPosition > 0) {
                    MusicPlayerGUI.this.volumeInDecibels = (float) (Math.log10(squaredValue) * 20.0);
                } else {
                    MusicPlayerGUI.this.volumeInDecibels = -80.0f; // Pure silence
                }
                
                // Update the frame in the music player
                musicPlayer.setMusicPlayerVolume(MusicPlayerGUI.this.volumeInDecibels);

                // Resume the current song (if there is a song in the musicplayer or song is stopped)
                if (musicPlayer.getCurrentSong() != null && musicPlayer.getPlayerIsPaused()){
                    musicPlayer.playCurrentSong();
                }
                
                // Toggle on the play button and off the pause button
                disablePlayButtonAndEnablePauseButton();

            }
        });
        add(volumeSlider);

        // Load no sound icon
        ImageIcon originalMuteIcon = loadImage("/noSound.png");
        // Create a scaled version (40x40 to match your setBounds)
        Image scaledImage = originalMuteIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        JLabel noSoundImage = new JLabel(new ImageIcon(scaledImage));
        noSoundImage.setBounds(335, ((getHeight()-150)/2), 25, 25);
        add(noSoundImage);

        // Load full sound icon
        ImageIcon originalFullSoundIcon = loadImage("/fullSoundVolume.png");
        // Create a scaled version (40x40 to match your setBounds)
        Image scaledFullSoundImage = originalFullSoundIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
        JLabel fullSoundImage = new JLabel(new ImageIcon(scaledFullSoundImage));
        fullSoundImage.setBounds(335, ((getHeight()-420)/2), 25, 25);
        add(fullSoundImage);

        // Playback buttons (i.e Next, Previous, Play)
        playbackButtons();
    }

    private void addToolbar(){
        JToolBar toolBar = new JToolBar();
        toolBar.setBounds(0, 0, getWidth(), 30);

        // Prevent toolbar from being moved
        toolBar.setFloatable(false);

        // Add drop down menu
        JMenuBar menuBar = new JMenuBar();
        toolBar.add(menuBar);

        // Add a song menu option
        JMenu songMenu = new JMenu("Songs");
        menuBar.add(songMenu);

        // Add sub menus for the SONG menu option
        // 1. Load new Song 
        JMenuItem loadSong = new JMenuItem("load Song");

        // Add an action popup to select a new song from the base folder
        loadSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                int result = fileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = fileChooser.getSelectedFile();

                if (result == JFileChooser.APPROVE_OPTION && selectedFile != null){

                    // Stop the current song
                    if (musicPlayer != null){
                        musicPlayer.isPaused = true;
                        if (!musicPlayer.getPlayerHasEnded()){
                            musicPlayer.stopSong();
                        }
                    }

                    // Create the music player instance
                    if(getFileExtension(selectedFile).equalsIgnoreCase("mp3")){
                        musicPlayer = new MusicPlayer(MusicPlayerGUI.this);
                        musicPlayer.setMusicPlayerVolume(MusicPlayerGUI.this.volumeInDecibels);
                    }
                    else if(getFileExtension(selectedFile).equalsIgnoreCase("wav")){
                        musicPlayer = new MusicPlayerWAV(MusicPlayerGUI.this);
                    }

                    // Create a song object
                    newSong = new Song(selectedFile.getPath());

                    // Update song Artwork
                    new Thread(() -> {
                        // This runs in the BACKGROUND
                        updateSongArtwork(newSong); 
                    }).start();
                    

                    // Load the song into the MusicPlayer
                    musicPlayer.loadSong(newSong);

                    // Update song title and artist
                    updateSongTitleAndArtist(newSong);

                    // Toggle off Play button and enable pause button
                    disablePlayButtonAndEnablePauseButton();

                    // Add the slider values
                    updatePlayBackSlider(newSong);

                }
            }
        });
        songMenu.add(loadSong);

        // Add play list menu option
        JMenu playListMenu = new JMenu("PlayList");
        menuBar.add(playListMenu);

        // Add sub menus for the PLAYLIST menu option
        // 1. Create new Playlist
        JMenuItem createPlayList = new JMenuItem("Create new Playlist");

        // Dialogue that appears when creating a new playlist
        createPlayList.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                // Load Playlist class
                MusicPlayerListDialog musicPlayerListDialog = new MusicPlayerListDialog(MusicPlayerGUI.this);
                musicPlayerListDialog.setVisible(true);
            }
            
        });
        playListMenu.add(createPlayList);

        // 2. Load new Playlist
        JMenuItem loadPlayList = new JMenuItem("Load Playlist");
        
        // Dialogue that appears when a new playlist is loaded
        loadPlayList.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                 // check to see if folder exists for playlists
                File playListDir = new File( "music-player\\assets" + File.separator + "playLists");

                // Create directory if it doesn't exist
                if (!playListDir.exists()) {
                    boolean created = playListDir.mkdirs();
                    if (!created) {
                        System.err.println("Failed to create playLists directory");
                    }
                }

                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileFilter(new FileNameExtensionFilter("PlayList", "txt"));
                jFileChooser.setCurrentDirectory(playListDir);

                int result = jFileChooser.showOpenDialog(MusicPlayerGUI.this);
                File selectedFile = jFileChooser.getSelectedFile();

                if (result == JFileChooser.APPROVE_OPTION && selectedFile != null){

                    // Create the music player instance
                    musicPlayer = new MusicPlayer(MusicPlayerGUI.this);

                    // Stop the music player
                    if (!musicPlayer.getPlayerHasEnded()){
                        musicPlayer.stopSong();
                    }

                    // Load the songs in the playlist
                    musicPlayer.loadPlayList(selectedFile);
                    newSong = musicPlayer.getCurrentSong();
                }
            }
            
        });
        playListMenu.add(loadPlayList);

        add(toolBar);
    }

    private void playbackButtons(){
        // Panel housing the different buttons
        this.playbackButtons = new JPanel();
        playbackButtons.setBounds(0, 435, getWidth()-10, 60);
        playbackButtons.setBackground(null);

        // Previous Button
        JButton previousButton = new JButton(loadImage("/previous.png"));
        previousButton.setBorderPainted(false);
        previousButton.setBackground(null);
        previousButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Play the previous song in the playlist
                musicPlayer.previousSong();
            }
            
        });
        playbackButtons.add(previousButton);

        // Play Button
        JButton playButton = new JButton(loadImage("/play.png"));
        playButton.setBorderPainted(false);
        playButton.setBackground(null);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                // Toggle the Play button off again
                disablePlayButtonAndEnablePauseButton();

                // Play the song from the point it was paused
                if (newSong != null){
                    musicPlayer.playCurrentSong();
                }

            }
        });
        playbackButtons.add(playButton);
        
        // Pause Button
        JButton pauseButton = new JButton(loadImage("/pause.png"));
        pauseButton.setBorderPainted(false);
        pauseButton.setBackground(null);
        pauseButton.setVisible(false);

        // Connecting the MusicPlayer.songPause() logic to the respective button
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                // Toggle the pause button off and the play button on
                disablePauseButtonAndEnablePlayButton();

                // Pause the song
                musicPlayer.pauseSong();
            }
        });

        playbackButtons.add(pauseButton);

        // Next Button
        JButton nextButton = new JButton(loadImage("/next.png"));
        nextButton.setBorderPainted(false);
        nextButton.setBackground(null);
        nextButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Play the next song in the playlist
                musicPlayer.nextSong();
            }
            
        });
        playbackButtons.add(nextButton); 
        
        add(playbackButtons);
    }

    public void updateSongTitleAndArtist(Song song){
        songArtist.setText(song.getSongArtists());
        songTitle.setText(song.getSongTitle());
    }

    public void updateSongArtwork(Song song){
        Map<String, String> songMetaData = musicDataService.getCompleteMetadata(song.getSongTitle(), song.getSongArtists());
        ImageIcon artworkImage;

        // Update the background shape colour
        this.lightingPanel.setGenreColor(songMetaData.get("genre"));

        System.out.println(songMetaData.get("artwork"));
        System.out.println(" ");

        try{
            if (songMetaData.get("artwork").startsWith("http")){
                artworkImage = new ImageIcon(new URL(songMetaData.get("artwork")));
                artworkImage = resizeIcon(artworkImage);
            }else{
                artworkImage = new ImageIcon(songMetaData.get("artwork").substring(5));
            }

            songImage.setIcon(artworkImage);
            songImage.revalidate();
            songImage.repaint();

        }catch (java.net.MalformedURLException e) {
            System.err.println("Bad URL format: " + e.getLocalizedMessage());
        }

    }
 
    public void setPlayBackSliderValue(int frame){
        // update the slider based on the song frame
        playBackSlider.setValue(frame);
    }

    public void updatePlayBackSlider(Song song){
        // Update maximum limit of the Slider
        playBackSlider.setMaximum((int)song.getMaximumFrameCount());

        // Create song length table (HashTabel)
        Hashtable<Integer, JLabel> lengthTable = new Hashtable<>();

        // Initialize all songs from 00:00
        JLabel lengthBeginning = new JLabel("00:00");
        lengthBeginning.setFont(new Font("Dialog", Font.BOLD, 18));
        lengthBeginning.setForeground(TEXT_COLOR);

        // End Length will vary based on song
        JLabel lengthEnd = new JLabel(song.getFormatedsongLength());
        lengthEnd.setFont(new Font("Dialog", Font.BOLD, 18));
        lengthEnd.setForeground(TEXT_COLOR);

        // Add enteries to label table
        lengthTable.put(0, lengthBeginning);
        lengthTable.put((int)song.getMaximumFrameCount(), lengthEnd);

        // Assign the values to the slider
        playBackSlider.setLabelTable(lengthTable);
        playBackSlider.setPaintLabels(true);

    }

    public void disablePlayButtonAndEnablePauseButton(){
        JButton playButton = (JButton) playbackButtons.getComponent(1);
        JButton pauseButton = (JButton) playbackButtons.getComponent(2);

        // Disable the play button
        playButton.setEnabled(false);
        playButton.setVisible(false);

        // Enable the pause button
        pauseButton.setEnabled(true);
        pauseButton.setVisible(true);

    }

    public void disablePauseButtonAndEnablePlayButton(){
        JButton playButton = (JButton) playbackButtons.getComponent(1);
        JButton pauseButton = (JButton) playbackButtons.getComponent(2);

        // Enable the play button
        playButton.setEnabled(true);
        playButton.setVisible(true);

        // Disable the pause button
        pauseButton.setEnabled(false);
        pauseButton.setVisible(false);

    }

    private ImageIcon loadImage(String imagePath){
        try{
            // Load the image icon from the file
            java.net.URL imageURL = getClass().getResource(imagePath);
            // Return the image object
            return new ImageIcon(imageURL);
        }catch(Exception e){
            System.out.println("Image not found");
        }

        return null;
        
    }

    private ImageIcon rotateImage(Image img, double degrees) {
        BufferedImage bufferedImage = new BufferedImage(
            img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g2d = bufferedImage.createGraphics();
        
        // Set rotation point to the center of the image
        g2d.rotate(Math.toRadians(degrees), img.getWidth(null) / 2.0, img.getHeight(null) / 2.0);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        
        return new ImageIcon(bufferedImage);
    }

    public ImageIcon resizeIcon(ImageIcon icon) {
        // Get the Image object from the ImageIcon
        Image originalImage = icon.getImage();

        //System.out.println(this.imageWidth);
        //System.out.println(this.imageHeight);

        // Create a scaled version
        Image resizedImage = originalImage.getScaledInstance(this.imageWidth, this.imageHeight, Image.SCALE_SMOOTH);

        // Wrap it back into an ImageIcon
        return new ImageIcon(resizedImage);
    }
}
