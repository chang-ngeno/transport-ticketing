package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank String name,
        @NotBlank String mpesaShortcode,
        @NotBlank String consumerKey,
        @NotBlank String consumerSecret,
        @NotBlank String passkey
) {}
