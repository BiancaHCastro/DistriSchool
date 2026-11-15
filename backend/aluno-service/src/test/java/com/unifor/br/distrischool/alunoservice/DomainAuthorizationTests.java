package com.unifor.br.distrischool.alunoservice;
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
@Autowired com.unifor.br.distrischool.alunoservice.repository.AlunoRepository repo;
 @BeforeEach void data(){repo.deleteAll();}
 @Test void studentSeesOnlyOwnProfileAndCannotMutate()throws Exception{
 var own=new com.unifor.br.distrischool.alunoservice.model.Aluno();own.setNome("Ana");own.setMatricula("TEST-1");own=repo.save(own);
 var other=new com.unifor.br.distrischool.alunoservice.model.Aluno();other.setNome("Outra pessoa");other.setMatricula("TEST-2");other=repo.save(other);
 mvc.perform(get("/alunos").with(as("ROLE_ALUNO",own.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nome").value("Ana"));
 mvc.perform(get("/alunos/"+other.getId()).with(as("ROLE_ALUNO",own.getId()))).andExpect(status().isForbidden());
 mvc.perform(delete("/alunos/"+own.getId()).with(as("ROLE_ALUNO",own.getId()))).andExpect(status().isForbidden());
 mvc.perform(get("/alunos").with(as("ROLE_ADMIN",1L))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
 }
}
