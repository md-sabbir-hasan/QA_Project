package com.nexaerp.user;

import com.nexaerp.user.dto.UserRequestDto;
import com.nexaerp.user.dto.UserResponseDto;

import java.util.List;

public interface UserService {
    UserResponseDto create(UserRequestDto request);
    UserResponseDto update(Long id, UserRequestDto request);
    UserResponseDto getById(Long id);
    List<UserResponseDto> getAll();
    void deactivate(Long id);
}
