package com.unifor.br.distrischool.nota_service;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Import(SecurityIntegrationTests.Probes.class)
class SecurityIntegrationTests {
 @Autowired MockMvc mvc;
 @Autowired SecretKeySpec key;
 @TestConfiguration static class Probes {@Bean Probe probe(){return new Probe();}}
 @RestController static class Probe {
  @GetMapping("/test/read") String read(){return "ok";}
  @PostMapping("/test/admin") @PreAuthorize("hasRole('ADMIN')") String admin(){return "ok";}
  @PostMapping("/test/professor") @PreAuthorize("hasRole('PROFESSOR')") String teacher(){return "ok";}
  @GetMapping("/test/own/{id}") @PreAuthorize("hasRole('ALUNO') and T(com.unifor.br.distrischool.nota_service.security.Identity).own(#id)") String own(@PathVariable Long id){return "ok";}
 }
 String token(String role,Instant expiry){var claims=JwtClaimsSet.builder().issuer("https://distrischool.local").subject("1").issuedAt(Instant.now().minusSeconds(120)).expiresAt(expiry).claim("roles",List.of(role)).claim("academicId",12L).build();return new NimbusJwtEncoder(new ImmutableSecret<>(key)).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();}
 String token(String role){return token(role,Instant.now().plusSeconds(3600));}
 @Test void missingTokenIs401()throws Exception{mvc.perform(get("/test/read")).andExpect(status().isUnauthorized());}
 @Test void invalidTokenIs401()throws Exception{mvc.perform(get("/test/read").header("Authorization","Bearer invalid.jwt.token")).andExpect(status().isUnauthorized());}
 @Test void expiredTokenIs401()throws Exception{mvc.perform(get("/test/read").header("Authorization","Bearer "+token("ROLE_ALUNO",Instant.now().minusSeconds(90)))).andExpect(status().isUnauthorized());}
 @Test void rolesAreEnforced()throws Exception{
  for(String role:List.of("ROLE_ADMIN","ROLE_PROFESSOR","ROLE_ALUNO")){
   mvc.perform(get("/test/read").header("Authorization","Bearer "+token(role))).andExpect(status().isOk());
   mvc.perform(post("/test/admin").header("Authorization","Bearer "+token(role))).andExpect(status().is(role.equals("ROLE_ADMIN")?200:403));
   mvc.perform(post("/test/professor").header("Authorization","Bearer "+token(role))).andExpect(status().is(role.equals("ROLE_PROFESSOR")?200:403));
  }
 }
 @Test void studentCannotReadAnotherProfile()throws Exception{var t=token("ROLE_ALUNO");mvc.perform(get("/test/own/12").header("Authorization","Bearer "+t)).andExpect(status().isOk());mvc.perform(get("/test/own/13").header("Authorization","Bearer "+t)).andExpect(status().isForbidden());}
 @Test void wrongSignatureIs401()throws Exception{var other=new SecretKeySpec("another-secret-at-least-thirty-two-bytes".getBytes(),"HmacSHA256");var claims=JwtClaimsSet.builder().issuer("https://distrischool.local").subject("1").expiresAt(Instant.now().plusSeconds(60)).build();var bad=new NimbusJwtEncoder(new ImmutableSecret<>(other)).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();mvc.perform(get("/test/read").header("Authorization","Bearer "+bad)).andExpect(status().isUnauthorized());}
}