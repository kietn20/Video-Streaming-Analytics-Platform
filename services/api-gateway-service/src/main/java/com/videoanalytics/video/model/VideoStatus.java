/**
 * Video Status Enum
 * Location: src/main/java/com/videoanalytics/video/model/VideoStatus.java
 *
 * Represents the various states a video can be in throughout its lifecycle
 * in the system.
 */
package com.videoanalytics.video.model;

public enum VideoStatus {
    PROCESSING,    // Video is being processed after upload
    READY,        // Video is processed and ready for viewing
    ERROR,        // Error occurred during processing
    DELETED       // Video has been marked as deleted
}