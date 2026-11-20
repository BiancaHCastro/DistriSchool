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
 @Test void situacaoMantemEscopoDoAluno() throws Exception {
  var nota = new com.unifor.br.distrischool.nota_service.model.Nota();
  nota.setAlunoId(24L);
  nota.setTurmaId("T-2");
  nota.setNota1(8.0);
  nota.setNota2(6.0);
  nota.setNotaFinal(0.0);
  repo.save(nota);
  mvc.perform(get("/api/notas/aluno/24/situacao").with(as("ROLE_ALUNO", 24L)))
          .andExpect(status().isOk()).andExpect(jsonPath("$[0].situacao").value("APROVADO"))
          .andExpect(jsonPath("$[0].mediaFinal").value(7.0));
  mvc.perform(get("/api/notas/aluno/24/situacao").with(as("ROLE_ALUNO", 25L)))
          .andExpect(status().isForbidden());
 }
 @Test void notaInexistenteRetorna404AoExcluir() throws Exception {
  mvc.perform(delete("/api/notas/{id}", Long.MAX_VALUE).with(as("ROLE_ADMIN", 1L)))
          .andExpect(status().isNotFound());
 }
 @Test void relatorioPdfRetornaArquivo() throws Exception {
  var response = mvc.perform(get("/api/notas/relatorio/export").param("formato", "pdf")
          .with(as("ROLE_ADMIN", 1L))).andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_PDF))
          .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("notas.pdf")))
          .andReturn().getResponse();
  org.junit.jupiter.api.Assertions.assertTrue(
          new String(response.getContentAsByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
 }
}
