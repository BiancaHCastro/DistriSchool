package com.unifor.br.distrischool.nota_service;
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
@Autowired com.unifor.br.distrischool.nota_service.repository.NotaRepository repo;
 @Test void studentReadsOnlyOwnGradesAndCannotSubmitGrades()throws Exception{
 repo.deleteAll();for(long id:new long[]{12,13}){var n=new com.unifor.br.distrischool.nota_service.model.Nota();n.setAlunoId(id);n.setTurmaId("T-1");n.setNota1(8.0);repo.save(n);}
 mvc.perform(get("/api/notas").with(as("ROLE_ALUNO",12L))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].alunoId").value(12));
 mvc.perform(get("/api/notas/aluno/13").with(as("ROLE_ALUNO",12L))).andExpect(status().isForbidden());
 mvc.perform(post("/api/notas").with(as("ROLE_ALUNO",12L)).contentType(MediaType.APPLICATION_JSON).content("{\"alunoId\":12,\"turmaId\":\"T-1\",\"nota1\":10}")).andExpect(status().isForbidden());
 }
}
