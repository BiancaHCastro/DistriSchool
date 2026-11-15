package com.unifor.br.distrischool.auth;
import jakarta.persistence.*;
@Entity @Table(name="auth_users")
public class UserAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public String email;
 @Column(nullable=false) public String name;
 @Column(nullable=false) public String passwordHash;
 @Column(nullable=false) public String role;
 public Long academicId;
}