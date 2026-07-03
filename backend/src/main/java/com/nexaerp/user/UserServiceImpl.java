package com.nexaerp.user;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.auth.AuthService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.role.Role;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.dto.UserRequestDto;
import com.nexaerp.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final AuthService authService;


    @Override
    @Transactional
    public UserResponseDto create(UserRequestDto request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + request.getEmail());
        }

        Set<Role> roles = getRoles(request.getRoleIds());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(null)           // No password yet
                .status(UserStatus.PENDING) // PENDING until password set
                .failedLoginAttempts(0)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        // Send invite email
        authService.sendInviteEmail(saved);

        // Audit log
        auditLogService.log(AuditAction.CREATED, "USER", saved.getId(),
                null, saved.getEmail());

        return toResponse(saved);
    }

    @Override
    public UserResponseDto update(Long id, UserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Email change--- unique check
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRoles(getRoles(request.getRoleIds()));

        User updated = userRepository.save(user);

        // Audit
        auditLogService.log(
                AuditAction.UPDATED,
                "USER",
                updated.getId(),
                null,
                updated.getEmail()
        );

        return toResponse(userRepository.save(user));
    }

    @Override
    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Override
    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        // Audit
        auditLogService.log(
                AuditAction.DEACTIVATED,
                "USER",
                user.getId(),
                "ACTIVE",
                "INACTIVE"
        );
    }


                                      // --Private Helper---++++##


    private Set<Role> getRoles(Set<Long> roleIds) {
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
            roles.add(role);
        }
        return roles;
    }

    private UserResponseDto toResponse(User user) {

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .status(user.getStatus())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(roleNames)
                .permissions(permissions)
                .build();
    }

}
