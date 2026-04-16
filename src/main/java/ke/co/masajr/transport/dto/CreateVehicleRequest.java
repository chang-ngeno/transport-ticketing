package ke.co.masajr.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(
        @NotBlank String registrationNumber,
        @NotNull @Min(1) Integer capacity,
        Long stageId    // optional override; defaults to caller's stageId
) {}
