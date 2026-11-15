package com.unifor.br.distrischool.auth;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Component @ConditionalOnProperty(name="app.seed.enabled",havingValue="true",matchIfMissing=true)
public class SeedUsers implements ApplicationRunner {
 private final UserRepository users; private final String password;
 public SeedUsers(UserRepository users,@Value("${SEED_PASSWORD:Universidade123!}") String password){this.users=users;this.password=password;}
 public void run(ApplicationArguments args){seed("admin@universidade.edu","Administrador","ROLE_ADMIN");seed("prof@universidade.edu","Docente","ROLE_PROFESSOR");seed("aluno@universidade.edu","Discente","ROLE_ALUNO");}
 private void seed(String email,String name,String role){if(users.findByEmail(email).isPresent())return;var u=new UserAccount();u.email=email;u.name=name;u.role=role;u.passwordHash=new BCryptPasswordEncoder(10).encode(password);users.save(u);}
}