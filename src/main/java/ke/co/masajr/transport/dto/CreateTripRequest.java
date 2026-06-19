package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTripRequest(
    @NotNull Long fromStageId,
    Long toStageId,
    Long vehicleId,
    String toDestination,
    String route,
    LocalDateTime tripStartTime,
    Integer totalSeats,
    @NotNull BigDecimal basePrice,
    Long tenantId
) {}
