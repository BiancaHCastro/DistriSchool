package com.unifor.br.api_gateway.controller;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.unifor.br.api_gateway.security.ResilientHttp;
@RestController
public class GatewayProxyController {
 private final ResilientHttp http;private final String auth,students,teachers,turmas,notes,attendance;
 public GatewayProxyController(ResilientHttp http,
 @Value("${AUTH_SERVICE_URL:http://auth-service:8081}") String auth,
 @Value("${ALUNO_SERVICE_URL:http://aluno-service:8082}") String students,
 @Value("${PROFESSOR_SERVICE_URL:http://professor-service:8083}") String teachers,
 @Value("${TURMA_SERVICE_URL:http://turma-service:8085}") String turmas,
 @Value("${NOTA_SERVICE_URL:http://nota-service:8086}") String notes,
 @Value("${ATTENDANCE_SERVICE_URL:http://attendance-service:8084}") String attendance){this.http=http;this.auth=auth;this.students=students;this.teachers=teachers;this.turmas=turmas;this.notes=notes;this.attendance=attendance;}
 @RequestMapping({"/api/students","/api/students/**","/api/teachers","/api/teachers/**","/api/turmas","/api/turmas/**","/api/disciplinas","/api/disciplinas/**","/api/matriculas","/api/matriculas/**","/api/notas","/api/notas/**","/api/avaliacoes","/api/avaliacoes/**","/api/academic/**","/api/attendance/**","/api/users","/api/users/**","/api/auth/**"})
 public ResponseEntity<?> proxy(HttpServletRequest request,@RequestBody(required=false) String body){
  String uri=request.getRequestURI(),target,service;
  if(uri.startsWith("/api/students")){target=students;service="aluno-service";}
  else if(uri.startsWith("/api/teachers")){target=teachers;service="professor-service";}
  else if(uri.startsWith("/api/turmas")||uri.startsWith("/api/disciplinas")||uri.startsWith("/api/matriculas")){target=turmas;service="turma-service";}
  else if(uri.startsWith("/api/notas")||uri.startsWith("/api/avaliacoes")){target=notes;service="nota-service";uri=uri.replace("/api/avaliacoes","/avaliacoes");}
  else if((uri.startsWith("/api/academic")||uri.startsWith("/api/attendance/"))){target=attendance;service="attendance-service";}
  else{target=auth;service="auth-service";}
  if(request.getQueryString()!=null)uri+="?"+request.getQueryString();
  var headers=new HttpHeaders();
  if(request.getHeader("Authorization")!=null)headers.set("Authorization",request.getHeader("Authorization"));
  if(request.getContentType()!=null)headers.set("Content-Type",request.getContentType());
  if (uri.contains("/relatorio/export")) {
   var file = http.exchange(service, target + uri, HttpMethod.valueOf(request.getMethod()),
       new HttpEntity<>(body, headers), byte[].class);
   var forwarded = new HttpHeaders();
   if (file.getHeaders().getContentType() != null) forwarded.setContentType(file.getHeaders().getContentType());
   var disposition = file.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
   if (disposition != null) forwarded.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
   return ResponseEntity.status(file.getStatusCode()).headers(forwarded).body(file.getBody());
  }
  var response=http.exchange(service,target+uri,HttpMethod.valueOf(request.getMethod()),new HttpEntity<>(body,headers));
  var result=new HttpHeaders();if(response.getHeaders().getContentType()!=null)result.setContentType(response.getHeaders().getContentType());
  return ResponseEntity.status(response.getStatusCode()).headers(result).body(response.getBody());
 }
}