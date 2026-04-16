package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStageRequest(
        @NotBlank String name,
        String location,
        Long tenantId   // used only by SUPER_ADMIN; ignored otherwise
) {}
