package com.nexaerp.user;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.user.dto.UserRequestDto;
import com.nexaerp.user.dto.UserResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> create(
            @Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("User created",
                userService.create(request)));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("User updated",
                userService.update(id, request)));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getById(id)));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getAll()));
    }

    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }
}
