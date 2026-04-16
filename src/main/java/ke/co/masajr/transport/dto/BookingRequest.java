package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record BookingRequest(
        @NotNull Long tripId,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone number") String phoneNumber
) {}
