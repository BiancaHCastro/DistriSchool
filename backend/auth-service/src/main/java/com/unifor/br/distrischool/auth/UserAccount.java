package com.unifor.br.distrischool.auth;
import jakarta.persistence.*;
@org.hibernate.annotations.DynamicUpdate
@Entity @Table(name="auth_users")
public class UserAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public String email;
 @Column(nullable=false) public String name;
 @Column(nullable=false) public String passwordHash;
 @Column(nullable=false) public String role;
 public Long academicId;
 @Column(nullable=false) public boolean emailVerified = true;
 public String verificationHash;
 public java.time.Instant verificationExpires;
 public String resetHash;
 public java.time.Instant resetExpires;
}