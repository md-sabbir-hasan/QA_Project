package com.nexaerp.settings.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettingUpdateRequestDto {
    @NotNull(message = "Account ID is required")
    private Long accountId;
}
