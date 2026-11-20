package com.unifor.br.distrischool.auth;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<UserAccount,Long> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 Optional<UserAccount> findFirstByVerificationHash(String hash);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 Optional<UserAccount> findFirstByResetHash(String hash);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 Optional<UserAccount> findFirstByEmail(String email);
 Optional<UserAccount> findFirstByAcademicIdAndRoleAndEmailVerifiedTrueOrderByIdAsc(Long academicId, String role);
 Optional<UserAccount> findByEmail(String email); }
