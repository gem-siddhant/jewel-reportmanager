package com.jewel.reportmanager.service;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Service
public class JwtHelperService {

    private final String SECRET_KEY = "secret";

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody().getSubject();
    }
}

