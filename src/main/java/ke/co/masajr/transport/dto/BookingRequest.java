package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BookingRequest(
    @NotNull Long tripId,
    @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Invalid phone number") String phoneNumber,
    @NotNull String paymentMethod  // "CASH" or "MPESA"
) {}
