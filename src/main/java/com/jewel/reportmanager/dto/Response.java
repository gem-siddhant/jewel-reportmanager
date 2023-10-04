package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jewel.reportmanager.enums.OperationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Response {

    private final Object data;
    private final String message;
    private final OperationType operation;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String subOperationType;

    public Response(Object data, String message, OperationType operation) {
        this.data = data;
        this.message = message;
        this.operation = operation;
        subOperationType = null;
    }
}
