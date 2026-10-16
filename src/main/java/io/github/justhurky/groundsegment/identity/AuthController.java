package io.github.justhurky.groundsegment.identity;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.identity.dto.LoginRequest;
import io.github.justhurky.groundsegment.identity.dto.TokenResponse;
import io.github.justhurky.groundsegment.identity.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UserService userService, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        Set<Role> roles = userService.rolesOf(authentication.getName());
        JwtService.IssuedToken token = jwtService.issue(authentication.getName(), roles);
        auditService.recordAs(authentication.getName(), AuditAction.USER_LOGIN_SUCCEEDED, "app-user",
                authentication.getName(), Map.of());
        return TokenResponse.of(token, roles);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return userService.profileOf(authentication.getName());
    }
}
