package com.Frank.client;

public interface DownloadProgressListener {
    void onProgressUpdate(double progress, long transferredBytes, long totalBytes, long elapsedTime);
}