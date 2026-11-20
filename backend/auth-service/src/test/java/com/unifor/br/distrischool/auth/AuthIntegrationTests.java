package com.unifor.br.distrischool.auth;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class AuthIntegrationTests {
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired JwtDecoder decoder;
 @MockitoBean LoginEvents events;
 @MockitoBean Notifications notifications;
 @BeforeEach void user(){users.deleteAll();var u=new UserAccount();u.email="admin@universidade.edu";u.name="Administrador";u.role="ROLE_ADMIN";u.passwordHash=new BCryptPasswordEncoder(10).encode("Universidade123!");users.save(u);}
 @Test void loginReturnsValidJwtAndNeverPassword()throws Exception{
  long start=System.nanoTime();var response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@universidade.edu\",\"password\":\"Universidade123!\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ROLE_ADMIN")).andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
  assertTrue((System.nanoTime()-start)/1_000_000<2000,"Login deve responder em menos de 2s");var data=new ObjectMapper().readTree(response);var jwt=decoder.decode(data.get("token").asText());assertEquals("https://distrischool.local",jwt.getIssuer().toString());assertTrue(jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN"));verify(events).publish(any());
 }
 @Test void badCredentialsAre401()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@universidade.edu\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());verify(events,never()).publish(any());}
 @Test void unknownUserIs401()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@universidade.edu\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());}
 @Test void invalidInputIs400()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"bad\",\"password\":\"\"}")).andExpect(status().isBadRequest());}
 @Test void registrationVerifiesAccountBeforeLogin() throws Exception {
  String registration = """
          {"name":"Novo aluno","email":"novo@example.org","password":"password123"}
          """;
  String credentials = """
          {"email":"novo@example.org","password":"password123"}
          """;
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
          .andExpect(status().isCreated()).andExpect(content().string(""));
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
          .andExpect(status().isConflict());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
          .andExpect(status().isForbidden());
  var link = org.mockito.ArgumentCaptor.forClass(String.class);
  verify(notifications).send(eq("VERIFICACAO_EMAIL"), eq("novo@example.org"), isNull(), anyString(), link.capture());
  String token = link.getValue().split("token=", 2)[1];
  String verification = new ObjectMapper().writeValueAsString(java.util.Map.of("token", token));
  mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(verification))
          .andExpect(status().isNoContent());
  mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(verification))
          .andExpect(status().isBadRequest());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
          .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ROLE_ALUNO"));
 }
 @Test void passwordResetRequestDoesNotExposeAccountExistence() throws Exception {
  for (String email : java.util.List.of("admin@universidade.edu", "unknown@example.org")) {
   mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
           .content(new ObjectMapper().writeValueAsString(java.util.Map.of("email", email))))
           .andExpect(status().isAccepted()).andExpect(content().string(""));
  }
 }
 @Test void verificationResendDoesNotExposeAccountExistence() throws Exception {
  for (String email : java.util.List.of("admin@universidade.edu", "unknown@example.org")) {
   mvc.perform(post("/api/auth/verify-email/resend").contentType(MediaType.APPLICATION_JSON)
           .content(new ObjectMapper().writeValueAsString(java.util.Map.of("email", email))))
           .andExpect(status().isAccepted()).andExpect(content().string(""));
  }
  verifyNoInteractions(notifications);
 }
 @Test void seedIsIdempotentAndHashesAllThreePasswords(){users.deleteAll();var seed=new SeedUsers(users,"Universidade123!");seed.run(null);seed.run(null);assertEquals(3,users.count());for(var u:users.findAll()){assertTrue(new BCryptPasswordEncoder().matches("Universidade123!",u.passwordHash));assertNotEquals("Universidade123!",u.passwordHash);}}
}
