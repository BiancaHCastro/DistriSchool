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
 @BeforeEach void user(){users.deleteAll();var u=new UserAccount();u.email="admin@universidade.edu";u.name="Administrador";u.role="ROLE_ADMIN";u.passwordHash=new BCryptPasswordEncoder(10).encode("Universidade123!");users.save(u);}
 @Test void loginReturnsValidJwtAndNeverPassword()throws Exception{
  long start=System.nanoTime();var response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@universidade.edu\",\"password\":\"Universidade123!\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("ROLE_ADMIN")).andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
  assertTrue((System.nanoTime()-start)/1_000_000<2000,"Login deve responder em menos de 2s");var data=new ObjectMapper().readTree(response);var jwt=decoder.decode(data.get("token").asText());assertEquals("https://distrischool.local",jwt.getIssuer().toString());assertTrue(jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN"));verify(events).publish(any());
 }
 @Test void badCredentialsAre401()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"admin@universidade.edu\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());verify(events,never()).publish(any());}
 @Test void unknownUserIs401()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@universidade.edu\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());}
 @Test void invalidInputIs400()throws Exception{mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"bad\",\"password\":\"\"}")).andExpect(status().isBadRequest());}
 @Test void seedIsIdempotentAndHashesAllThreePasswords(){users.deleteAll();var seed=new SeedUsers(users,"Universidade123!");seed.run(null);seed.run(null);assertEquals(3,users.count());for(var u:users.findAll()){assertTrue(new BCryptPasswordEncoder().matches("Universidade123!",u.passwordHash));assertNotEquals("Universidade123!",u.passwordHash);}}
}