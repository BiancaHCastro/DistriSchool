package com.unifor.br.distrischool.nota_service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.unifor.br.distrischool.nota_service.security.ResilientHttp;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
@SpringBootTest @AutoConfigureMockMvc
class DependencyFailureTests {
 @Autowired MockMvc mvc; @MockitoBean ResilientHttp http;
 @Test void unavailablePermissionServiceReturns503AndDoesNotGrantAccess()throws Exception{
  when(http.exchange(anyString(),anyString(),any(),any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Serviço temporariamente indisponível"));
  mvc.perform(post("/api/notas").with(jwt().jwt(j->j.claim("roles",java.util.List.of("ROLE_PROFESSOR")).claim("academicId",12L)).authorities(new SimpleGrantedAuthority("ROLE_PROFESSOR"))).contentType(MediaType.APPLICATION_JSON).content("{\"alunoId\":1,\"turmaId\":\"T-1\",\"nota1\":8}")).andExpect(status().isServiceUnavailable());
 }
}