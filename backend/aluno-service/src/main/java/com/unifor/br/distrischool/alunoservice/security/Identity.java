package com.unifor.br.distrischool.alunoservice.security;
import java.util.Objects;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
public final class Identity {
    private Identity() {}
    public static Jwt jwt() { return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    public static boolean role(String role) { return jwt().getClaimAsStringList("roles").contains("ROLE_" + role); }
    public static Long academicId() { var id = jwt().getClaim("academicId"); return id == null ? null : Long.valueOf(id.toString()); }
    public static boolean own(Long id) { return id != null && Objects.equals(id, academicId()); }
    public static String token() { return jwt().getTokenValue(); }
}