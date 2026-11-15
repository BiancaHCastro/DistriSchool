package com.unifor.br.distrischool.alunoservice.security;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component("academicAccess")
public class AcademicAccess {
 private final ResilientHttp http; private final String url;
 public AcademicAccess(ResilientHttp http,@Value("${TURMA_SERVICE_URL:http://turma-service:8085}") String url){this.http=http;this.url=url;}
 public record Scope(List<String> turmaIds,List<Long> alunoIds){}
 public Scope scope(){
  var headers=new HttpHeaders();headers.setBearerAuth(Identity.token());
  var result=http.exchange("turma-service",url+"/api/turmas/permissoes",HttpMethod.GET,new HttpEntity<>(headers));
  if(!result.getStatusCode().is2xxSuccessful())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Vínculo acadêmico não autorizado");
  try{return new ObjectMapper().readValue(result.getBody(),Scope.class);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível verificar o vínculo acadêmico");}
 }
 public boolean student(Long id){return Identity.role("ADMIN") || (Identity.role("ALUNO")&&Identity.own(id)) || (Identity.role("PROFESSOR")&&scope().alunoIds().contains(id));}
 public boolean turma(String id){return Identity.role("ADMIN")||(id!=null&&scope().turmaIds().contains(id));}
 public boolean write(Long aluno,String turma){return Identity.role("ADMIN")||(Identity.role("PROFESSOR")&&authorized(aluno,turma));}
 private boolean authorized(Long aluno,String turma){
  if(turma==null||aluno==null)return false;
  var headers=new HttpHeaders();headers.setBearerAuth(Identity.token());
  var response=http.exchange("turma-service",url+"/api/turmas/autorizacao?turma="+java.net.URLEncoder.encode(turma,java.nio.charset.StandardCharsets.UTF_8)+"&alunoId="+aluno,HttpMethod.GET,new HttpEntity<>(headers));
  return response.getStatusCode().is2xxSuccessful();
 }
}