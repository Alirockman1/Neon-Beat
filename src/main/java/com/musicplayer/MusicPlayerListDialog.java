package com.musicplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.mpatric.mp3agic.FileWrapper;

public class MusicPlayerListDialog extends JDialog{
    
    private MusicPlayerGUI musicPlayerGUI;
    private ArrayList<String> songPaths;

    public MusicPlayerListDialog(MusicPlayerGUI musicPlayerGUI){
        this.musicPlayerGUI = musicPlayerGUI;
        this.songPaths = new ArrayList<>();

        // Initialize the dialog properties
        setTitle("Create Playlist");
        setSize(350, 350);
        setResizable(false);
        getContentPane().setBackground(MusicPlayerGUI.FRAME_COLOR);
        setLayout(null);
        
        // Enable modaql behavior - such it is importnat to close the dialogue before proceeding
        setModal(true);

        setLocationRelativeTo(musicPlayerGUI);

        addDialogComponents();
    }

    private void addDialogComponents(){
        // Container to store list song data
        JPanel songContainer = new JPanel();
        songContainer.setLayout(new BoxLayout(songContainer, BoxLayout.Y_AXIS));
        songContainer.setBounds((int) (getWidth() * 0.025), 10, (int) (getWidth() * 0.9), (int) (getHeight() * 0.75));
        add(songContainer);

        // Button to load/add new song
        JButton addSongButton = new JButton("Add");
        Color originalColor = addSongButton.getBackground(); // store original color

        addSongButton.setBounds(60, (int) (getHeight() * 0.80), 100, 25);
        addSongButton.setFont(new Font("Dialog", Font.BOLD, 14));

        // Action to perform when the add button is pressed
        addSongButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Step 1: Change color immediately
                addSongButton.setBackground(Color.ORANGE);

                // Step 2: Perform your action in a background thread (so UI stays responsive)
                new Thread(() -> {
                    try {
                        // Simulate your button function
                        
                        // Open file explorer
                        JFileChooser jFileChooser = new JFileChooser();
                        jFileChooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));
                        jFileChooser.setCurrentDirectory(new File(musicPlayerGUI.getFileDirectory()));
                        int result = jFileChooser.showOpenDialog(MusicPlayerListDialog.this);

                        File selectedFile = jFileChooser.getSelectedFile();

                        if(result == JFileChooser.APPROVE_OPTION && selectedFile != null){
                            JLabel filePathLabel = new JLabel(selectedFile.getPath());
                            filePathLabel.setFont(new Font("Dialog", Font.BOLD, 12));
                            filePathLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                            
                            // Add song path to the library
                            songPaths.add(filePathLabel.getText());

                            // Display song in the container
                            Song addedSong = new Song(selectedFile.getPath());
                            String artist = addedSong.getSongArtists();
                            if (artist.isEmpty()){
                                artist = "Unknown Artist";
                            }

                            String title = addedSong.getSongTitle();
                            if (title.isEmpty()){
                                title = "N/A";
                            }

                            JLabel songLabel = new JLabel( title + " - " + artist );
                            songContainer.add(songLabel);
                            songContainer.revalidate();
                        }
                    } finally {
                        // Step 3: Restore original color safely on the EDT
                        SwingUtilities.invokeLater(() -> {
                            addSongButton.setBackground(originalColor);
                        });
                    }
                }).start();

            }
        });
        add(addSongButton);
    
        // Button to save playlist
        JButton savePlayListButton = new JButton("Save");
        savePlayListButton.setBounds(215, (int) (getHeight() * 0.80), 100, 25);
        savePlayListButton.setFont(new Font("Dialog", Font.BOLD, 14));
        savePlayListButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                try{
                    // check to see if folder exists for playlists
                    File playListDir = new File(musicPlayerGUI.getFileDirectory() + File.separator + "playLists");

                    // Create directory if it doesn't exist
                    if (!playListDir.exists()) {
                        boolean created = playListDir.mkdirs();
                        if (!created) {
                            System.err.println("Failed to create playLists directory");
                        }
                    }

                    JFileChooser jFileChooser = new JFileChooser();
                    jFileChooser.setCurrentDirectory(playListDir);
                    int result = jFileChooser.showSaveDialog(MusicPlayerListDialog.this);

                    if (result == JFileChooser.APPROVE_OPTION){
                        // Get reference to the selected file to be saved
                        File selectedFile = jFileChooser.getSelectedFile();

                        if (!selectedFile.getName().substring(selectedFile.getName().length() - 4).equalsIgnoreCase(".txt")){
                            selectedFile = new File(selectedFile.getAbsoluteFile() + ".txt");

                        }

                        // Create new file
                        selectedFile.createNewFile();

                        // Write each song in the selected file
                        FileWriter fileWriter = new FileWriter(selectedFile);
                        BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
                        
                        // Iterate through the song paths and write each song in a new line
                        for(String songPath : songPaths){
                            fileWriter.write(songPath + "\n");
                        }
                        bufferWriter.close();

                        // Display a complete message pop up
                        JOptionPane.showMessageDialog(MusicPlayerListDialog.this, "Playlist successfully saved!");

                        //close the dialog
                        MusicPlayerListDialog.this.dispose();
                    }

                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
            
        });
         
        add(savePlayListButton);

    }
}
