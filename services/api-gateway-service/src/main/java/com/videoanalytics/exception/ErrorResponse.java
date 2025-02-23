/**
 * Error Response
 * Location: src/main/java/com/videoanalytics/exception/ErrorResponse.java
 *
 * This class represents the standardized error response structure for the API.
 */
package com.videoanalytics.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Object details;
}