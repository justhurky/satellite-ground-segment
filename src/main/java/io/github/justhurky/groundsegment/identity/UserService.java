package io.github.justhurky.groundsegment.identity;

import io.github.justhurky.groundsegment.audit.AuditAction;
import io.github.justhurky.groundsegment.audit.AuditService;
import io.github.justhurky.groundsegment.common.error.DuplicateResourceException;
import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.identity.dto.CreateUserRequest;
import io.github.justhurky.groundsegment.identity.dto.UserResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("An account named " + request.username() + " already exists");
        }
        AppUser user = userRepository.save(new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.roles(),
                clock.instant()));
        auditService.record(AuditAction.USER_CREATED, "app-user", user.getId().toString(),
                Map.of("roles", request.roles().stream().map(Role::name).sorted().toList().toString()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("username")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse profileOf(String username) {
        return UserResponse.from(requireByUsername(username));
    }

    @Transactional(readOnly = true)
    public AppUser requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("app-user", username));
    }

    @Transactional(readOnly = true)
    public Set<Role> rolesOf(String username) {
        return requireByUsername(username).getRoles();
    }
}
