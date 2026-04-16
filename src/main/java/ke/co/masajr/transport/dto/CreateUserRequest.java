package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ke.co.masajr.transport.entity.Role;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role,
        Long tenantId,
        Long stageId
) {}
