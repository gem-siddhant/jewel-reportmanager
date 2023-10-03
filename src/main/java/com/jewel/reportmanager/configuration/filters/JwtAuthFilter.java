package com.jewel.reportmanager.configuration.filters;

import com.jewel.reportmanager.utils.ReportResponseConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JwtAuthFilter implements HandlerInterceptor {

    private final String SECRET_KEY = "secret";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Class<?> controllerClass = handlerMethod.getBeanType();

            // Check if the request is going to ModuleController
            if (controllerClass.getSimpleName().equals("ModuleController")) {
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                    throw new UnAuthorizedException();
                }
                String token = authorizationHeader.substring(7);
                try {
                    Claims claims = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
                    MDC.put(ReportResponseConstants.USER_NAME_KEY, claims.getSubject().toString());
                    //MDC.put(Constants.COMPANY_ID_KEY, claims.get(Constants.COMPANY_ID_KEY).toString());
                    request.setAttribute("claims", claims);
                } catch (JwtException exception) {
                    throw new UnAuthorizedException();
                }
            }
        }

        return true;
    }
}

