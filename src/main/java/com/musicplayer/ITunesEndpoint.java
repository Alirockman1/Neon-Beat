package com.musicplayer;

public enum ITunesEndpoint {
    SEARCH("https://itunes.apple.com/search?"),
    LOOKUP("https://itunes.apple.com/lookup?");

    private final String url;

    // Constructor for the enum
    ITunesEndpoint(String url) {
        this.url = url;
    }

    /**
     * @return The base URL for the specific endpoint
     */
    public String getUrl() {
        return this.url;
    }

    @Override
    public String toString() {
        return this.url;
    }
}