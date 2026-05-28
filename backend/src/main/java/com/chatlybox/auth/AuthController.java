package com.chatlybox.auth;

import com.chatlybox.users.AppUser;
import com.chatlybox.users.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;

  public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/login")
  UserResponse login(@Valid @RequestBody LoginRequest request) {
    AppUser user = users.findByEmail(request.email().toLowerCase())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return UserResponse.from(user);
  }

  @GetMapping("/me")
  UserResponse me(Principal principal) {
    if (principal == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    return users.findByEmail(principal.getName().toLowerCase())
        .map(UserResponse::from)
        .orElseGet(() -> new UserResponse(null, principal.getName(), null, "ADMIN"));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout() {
  }

  record LoginRequest(@Email String email, @NotBlank String password) {}

  public record UserResponse(Object id, String email, String name, String role) {
    static UserResponse from(AppUser user) {
      return new UserResponse(user.id, user.email, null, user.role);
    }
  }
}
