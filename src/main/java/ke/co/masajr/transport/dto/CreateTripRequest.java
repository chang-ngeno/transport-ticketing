package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTripRequest(
        @NotNull Long fromStageId,
        @NotBlank String toDestination,
        String route,
        @NotNull LocalDateTime departureTime,
        @NotNull @Min(1) Integer totalSeats,
        @NotNull BigDecimal basePrice,
        Long tenantId   // used only by SUPER_ADMIN
) {}
