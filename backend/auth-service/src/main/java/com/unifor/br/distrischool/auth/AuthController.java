package com.unifor.br.distrischool.auth;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class AuthController {
 private final UserRepository users; private final JwtService jwt; private final LoginEvents events;
 private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(10);
 private final String dummyHash=passwords.encode("invalid-password-placeholder");
 public AuthController(UserRepository users,JwtService jwt,LoginEvents events){this.users=users;this.jwt=jwt;this.events=events;}
 public record Credentials(@NotBlank @Email String email,@NotBlank String password){}
 public record UserInput(@NotBlank @Email String email,@NotBlank String name,String password,@Pattern(regexp="ROLE_ADMIN|ROLE_PROFESSOR|ROLE_ALUNO") @NotNull String role,Long academicId){}
 public record UserView(Long id,String email,String name,String role,Long academicId){static UserView of(UserAccount u){return new UserView(u.id,u.email,u.name,u.role,u.academicId);}}
 public record Session(String token,UserView user){}
 @PostMapping("/api/auth/login")
 public Session login(@Valid @RequestBody Credentials input){
  var user=users.findByEmail(input.email().trim().toLowerCase()).orElse(null);
  boolean valid=passwords.matches(input.password(),user==null?dummyHash:user.passwordHash);
  if(user==null||!valid)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos");
  var token=jwt.issue(user);events.publish(user);return new Session(token,UserView.of(user));
 }
 @GetMapping("/api/auth/me") public UserView me(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken auth){return users.findById(Long.valueOf(auth.getName())).map(UserView::of).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 @GetMapping("/api/users") @PreAuthorize("hasRole('ADMIN')") public List<UserView> list(){return users.findAll().stream().map(UserView::of).toList();}
 @GetMapping("/api/users/{id}") @PreAuthorize("hasRole('ADMIN')") public UserView get(@PathVariable Long id){return UserView.of(find(id));}
 @PostMapping("/api/users") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
 public UserView create(@Valid @RequestBody UserInput input){return save(new UserAccount(),input);}
 @PutMapping("/api/users/{id}") @PreAuthorize("hasRole('ADMIN')") public UserView update(@PathVariable Long id,@Valid @RequestBody UserInput input){return save(find(id),input);}
 @DeleteMapping("/api/users/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){users.delete(find(id));}
 private UserAccount find(Long id){return users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado"));}
 private UserView save(UserAccount user,UserInput input){
  String email=input.email().trim().toLowerCase();
  users.findByEmail(email).filter(u->!Objects.equals(u.id,user.id)).ifPresent(u->{throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail já cadastrado");});
  if(user.id==null&&(input.password()==null||input.password().length()<8))throw new IllegalArgumentException("Senha deve conter pelo menos 8 caracteres");
  if(input.password()!=null&&!input.password().isBlank()){if(input.password().length()<8||input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Senha deve conter de 8 caracteres a 72 bytes");user.passwordHash=passwords.encode(input.password());}
  user.email=email;user.name=input.name();user.role=input.role();user.academicId="ROLE_ADMIN".equals(input.role())?null:input.academicId();return UserView.of(users.save(user));
 }
}