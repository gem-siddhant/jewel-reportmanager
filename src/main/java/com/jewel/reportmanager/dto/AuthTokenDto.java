package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

@Getter
@Setter
public class AuthTokenDto {

    @Transient
    public static final String SEQUENCE_NAME = "token_id";

    private long tokenId;

    private String username;

    private String bridgeToken;

    private long insertTime;

    private long expirationTime;

    private int status;

}
