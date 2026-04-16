package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchBookingRequest(
        @NotNull Long tripId,
        @NotEmpty List<String> phoneNumbers
) {}
