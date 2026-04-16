package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateFareRequest(
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,      // null = open-ended
        @NotNull BigDecimal price
) {}
