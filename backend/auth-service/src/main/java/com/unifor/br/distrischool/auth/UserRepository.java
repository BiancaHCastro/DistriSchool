package com.unifor.br.distrischool.auth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<UserAccount,Long> { Optional<UserAccount> findByEmail(String email); }
