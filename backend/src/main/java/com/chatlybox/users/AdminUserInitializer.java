package com.chatlybox.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final String adminEmail;
  private final String adminPassword;

  public AdminUserInitializer(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      @Value("${chatly.auth.admin-email}") String adminEmail,
      @Value("${chatly.auth.admin-password}") String adminPassword) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.adminEmail = adminEmail.toLowerCase();
    this.adminPassword = adminPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    users.findByEmail(adminEmail).orElseGet(() -> {
      AppUser user = new AppUser();
      user.email = adminEmail;
      user.passwordHash = passwordEncoder.encode(adminPassword);
      user.role = "ADMIN";
      return users.save(user);
    });
  }
}
