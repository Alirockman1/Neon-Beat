package com.musicplayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;


public class app {

    static {
        // Disable jaudiotagger logging completely
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    public static void main(String[] args) {
        
        // Start the Python Service
        int server = startUvicorn();

        if (server != 0){
            startMusicPlayerGUI();
        }

    }

    public static int startUvicorn() {
        
        String projectRoot = System.getProperty("user.dir");
        String serverFolderBasePath = "src/main/python/com/music_database_API";
        
        try {
            
            String serverFolderPath = Paths.get(projectRoot, serverFolderBasePath).toString();
            String venvPythonPath = Paths.get(projectRoot, ".venv", "Scripts", "python.exe").toString();

            // Setup the ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(
                venvPythonPath,
                "-m", "uvicorn", "main:app", "--reload"
            );

            // Set the working directory to the server folder
            processBuilder.directory(new File(serverFolderPath));

            // Redirect errors to your Java console so you can see why it fails
            processBuilder.inheritIO();

            // Start the process in the background
            Process serverProcess = processBuilder.start();
            System.out.println("\nUvicorn server started in background...");

            return 1;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static void startMusicPlayerGUI(){
        
        MusicPlayerGUI player = new MusicPlayerGUI();

        player.setVisible(true);
    }

}
