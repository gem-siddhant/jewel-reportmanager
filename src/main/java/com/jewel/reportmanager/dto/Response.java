package com.jewel.reportmanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jewel.reportmanager.enums.OperationType;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Response {

    private Object data;
    private String message;
    private OperationType operation;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private  String subOperationType;

    public Response(Object data, String message, OperationType operation) {
        this.data = data;
        this.message = message;
        this.operation = operation;
        subOperationType = null;
    }
}
