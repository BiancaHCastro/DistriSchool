package com.unifor.br.api_gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"}, allowCredentials = "true", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class GatewayProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/api/students/**")
    public ResponseEntity<Object> proxyStudents(HttpServletRequest request, @RequestBody(required = false) String body) {
        String uri = request.getRequestURI().replace("/api/students", "/alunos");
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += "?" + queryString;
        }
        return forwardRequest("http://aluno-service:8082" + uri, request, body);
    }

    @RequestMapping("/api/academic/**")
    public ResponseEntity<Object> proxyAcademic(HttpServletRequest request, @RequestBody(required = false) String body) {
        String uri = request.getRequestURI().replace("/api/academic", "");
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += "?" + queryString;
        }
        return forwardRequest("http://attendance-service:8084" + uri, request, body);
    }

    @RequestMapping(value = {"/api/users/**", "/api/auth/**"})
    public ResponseEntity<Object> proxyLogin(HttpServletRequest request, @RequestBody(required = false) String body) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += "?" + queryString;
        }
        return forwardRequest("http://login:8081" + uri, request, body);
    }

    private ResponseEntity<Object> forwardRequest(String targetUrl, HttpServletRequest request, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host")) {
                    headers.set(headerName, request.getHeader(headerName));
                }
            }

            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Object> response = restTemplate.exchange(URI.create(targetUrl), method, entity, Object.class);
            Object respBody = response.getBody();
            if (respBody == null) {
                respBody = "";
            }
            return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(respBody);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\": \"Gateway proxy error: " + e.getMessage() + "\"}");
        }
    }
}
