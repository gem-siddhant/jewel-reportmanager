package com.jewel.reportmanager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus httpStatus;

    /**
     * Create a new CustomException.
     *
     * @param message    A description of the exception.
     * @param httpStatus The HTTP status code to be returned.
     */
    public CustomException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
