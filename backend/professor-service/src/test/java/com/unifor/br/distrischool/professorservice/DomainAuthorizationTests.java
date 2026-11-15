package com.unifor.br.distrischool.professorservice;
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
@Autowired com.unifor.br.distrischool.professorservice.repository.ProfessorRepository repo;
 @Test void teacherCanEditOwnProfileButCannotEditAnother()throws Exception{
 repo.deleteAll();var own=new com.unifor.br.distrischool.professorservice.model.Professor();own.setNome("Docente");own.setEmail("own@universidade.edu");own=repo.save(own);
 var other=new com.unifor.br.distrischool.professorservice.model.Professor();other.setNome("Outro docente");other.setEmail("other@universidade.edu");other=repo.save(other);
 String payload="{\"nome\":\"Docente atualizado\",\"email\":\"own@universidade.edu\",\"telefone\":\"123\"}";
 mvc.perform(put("/api/teachers/"+own.getId()).with(as("ROLE_PROFESSOR",own.getId())).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Docente atualizado"));
 mvc.perform(put("/api/teachers/"+other.getId()).with(as("ROLE_PROFESSOR",own.getId())).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
 mvc.perform(delete("/api/teachers/"+own.getId()).with(as("ROLE_PROFESSOR",own.getId()))).andExpect(status().isForbidden());
 }
}
