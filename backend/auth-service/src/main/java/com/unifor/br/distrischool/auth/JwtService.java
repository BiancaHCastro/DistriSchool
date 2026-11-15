package com.unifor.br.distrischool.auth;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
@Service
public class JwtService {
 private final JwtEncoder encoder;
 public JwtService(SecretKeySpec key) { encoder=new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
 public String issue(UserAccount user) {
  var now=Instant.now();
  var claims=JwtClaimsSet.builder().issuer("https://distrischool.local").subject(user.id.toString()).issuedAt(now).expiresAt(now.plusSeconds(3600))
    .claim("name",user.name).claim("email",user.email).claim("roles",List.of(user.role));
  if(user.academicId!=null) claims.claim("academicId",user.academicId);
  return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims.build())).getTokenValue();
 }
}