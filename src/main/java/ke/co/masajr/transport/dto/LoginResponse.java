package ke.co.masajr.transport.dto;

public record LoginResponse(
        String token,
        String role,
        Long tenantId
) {}
