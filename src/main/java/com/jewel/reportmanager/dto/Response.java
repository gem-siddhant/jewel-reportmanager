package com.jewel.reportmanager.dto;

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

}
