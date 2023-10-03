package com.jewel.reportmanager.exception;

import com.jewel.reportmanager.enums.OperationType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomDataException extends RuntimeException{

    private transient Object data;
    private final OperationType operationType;
    private final HttpStatus httpStatus;
    private final String  subOperationType;

    /**
     * Create a new CustomDataException.
     *
     * @param message         A description of the exception.
     * @param data            Additional data associated with the exception.
     * @param operationType   Operation type of the request.
     * @param httpStatus      The HTTP status code to be returned.
     */
    public CustomDataException(String message, Object data, OperationType operationType, HttpStatus httpStatus) {
        super(message);
        this.data = data;
        this.operationType = operationType;
        this.httpStatus = httpStatus;
        subOperationType = null;
    }

    /**
     * Create a new CustomDataException.
     *
     * @param message           A description of the exception.
     * @param data              Additional data associated with the exception.
     * @param operationType     Operation type of the request.
     * @param httpStatus        The HTTP status code to be returned.
     * @param subOperationType  subOperation type of the request.
     */
    public CustomDataException(String message, Object data, OperationType operationType, HttpStatus httpStatus, String subOperationType) {
        super(message);
        this.data = data;
        this.operationType = operationType;
        this.httpStatus = httpStatus;
        this.subOperationType = subOperationType;
    }

}
