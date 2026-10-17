package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarClaveRequest(
        @NotBlank String claveActual,
        @NotBlank
        @Size(min = 4, max = 72)
        String nuevaClave) {
}
