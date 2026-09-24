package com.tickets.security;

import com.tickets.api.LoginRequest;
import com.tickets.api.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final SecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            SecurityProperties properties,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        boolean passwordMatches = properties.operatorPassword().equals(request.password());
        boolean usernameMatches = properties.operatorUsername().equals(request.username());
        if (!usernameMatches || !passwordMatches) {
            throw new BadCredentialsException("Invalid operator credentials");
        }

        return new LoginResponse(jwtService.issueToken(properties.operatorUsername()), BEARER_TOKEN_TYPE);
    }
}
