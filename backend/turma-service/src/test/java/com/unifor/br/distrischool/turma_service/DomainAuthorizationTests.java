package com.unifor.br.distrischool.turma_service;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class DomainAuthorizationTests {
 @Autowired MockMvc mvc;
 JwtRequestPostProcessor as(String role,Long id){return jwt().jwt(j->j.subject("1").claim("roles",java.util.List.of(role)).claim("academicId",id)).authorities(new SimpleGrantedAuthority(role));}
@Autowired com.unifor.br.distrischool.turma_service.repository.TurmaRepository turmas;@Autowired com.unifor.br.distrischool.turma_service.repository.MatriculaTurmaRepository matriculas;
 @Test void teacherReadsOnlyAssignedClassesAndStudents()throws Exception{
 matriculas.deleteAll();turmas.deleteAll();var own=turmas.save(new com.unifor.br.distrischool.turma_service.model.Turma("OWN","Sistemas Distribuídos","2026.2","",40,12L));var other=turmas.save(new com.unifor.br.distrischool.turma_service.model.Turma("OTHER","Redes","2026.2","",40,13L));
 mvc.perform(get("/api/turmas").with(as("ROLE_PROFESSOR",12L))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].codigo").value("OWN"));
 mvc.perform(get("/api/turmas/"+other.getId()+"/alunos").with(as("ROLE_PROFESSOR",12L))).andExpect(status().isForbidden());
 mvc.perform(post("/api/turmas").with(as("ROLE_PROFESSOR",12L)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
 matriculas.save(new com.unifor.br.distrischool.turma_service.model.MatriculaTurma(own.getId(),22L));
 mvc.perform(get("/api/turmas/autorizacao?turma=OWN&alunoId=22").with(as("ROLE_ALUNO",22L))).andExpect(status().isForbidden());
 mvc.perform(get("/api/turmas/autorizacao?turma=OWN&alunoId=22").with(as("ROLE_PROFESSOR",12L))).andExpect(status().isOk());
 mvc.perform(get("/api/turmas/autorizacao?turma=OTHER&alunoId=22").with(as("ROLE_PROFESSOR",12L))).andExpect(status().isForbidden());
 }
 @Test void onlyStudentCanRequestOwnEnrollment()throws Exception{
 var t=turmas.save(new com.unifor.br.distrischool.turma_service.model.Turma("REQUEST","Banco de Dados","2026.2","",40,12L));String payload="{\"turmaId\":"+t.getId()+"}";
 mvc.perform(post("/api/matriculas").with(as("ROLE_ALUNO",22L)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated()).andExpect(jsonPath("$.alunoId").value(22)).andExpect(jsonPath("$.status").value("PENDENTE"));
 mvc.perform(post("/api/matriculas").with(as("ROLE_PROFESSOR",12L)).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
 }
}
