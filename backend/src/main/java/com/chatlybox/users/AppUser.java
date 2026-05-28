package com.chatlybox.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
  @Id
  public UUID id = UUID.randomUUID();

  public String email;

  @Column(name = "password_hash")
  public String passwordHash;

  public String role = "ADMIN";

  @Column(name = "created_at")
  public Instant createdAt = Instant.now();
}
