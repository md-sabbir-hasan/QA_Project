package com.nexaerp.party;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.party.dto.PartyRequestDto;
import com.nexaerp.party.dto.PartyResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PartyController {
    private final PartyService partyService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PARTY')")
    public ResponseEntity<ApiResponse<PartyResponseDto>> create(
            @Valid @RequestBody PartyRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Party created",
                partyService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PARTY')")
    public ResponseEntity<ApiResponse<PartyResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(partyService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PARTY')")
    public ResponseEntity<ApiResponse<List<PartyResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(partyService.getAll()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('VIEW_PARTY')")
    public ResponseEntity<ApiResponse<List<PartyResponseDto>>> getByType(
            @PathVariable PartyType type) {
        return ResponseEntity.ok(ApiResponse.success(partyService.getByType(type)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_PARTY')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        partyService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Party deactivated", null));
    }

}
