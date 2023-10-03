package com.jewel.reportmanager.utils;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class RestClient {
    private static final RestTemplate rest = new RestTemplate();

    public static ResponseEntity getApi(String url, HttpEntity httpEntity,  Class responseType, Map<String, Object> uriVariables) {

        return rest.exchange(url, HttpMethod.GET, httpEntity, responseType, uriVariables);
    }

    public static ResponseEntity getApi(String url, HttpEntity httpEntity, ParameterizedTypeReference<?> responseType, Map<String, Object> uriVariables) {

        return rest.exchange(url, HttpMethod.GET, httpEntity, responseType, uriVariables);
    }

    public static ResponseEntity putApi(String url, HttpEntity httpEntity, Class responseType, Map<String, Object> uriVariables) {

        return rest.exchange(url, HttpMethod.PUT, httpEntity, responseType, uriVariables);
    }
}
