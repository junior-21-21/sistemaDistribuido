package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarEstadoBimestreRequest(
        @NotBlank @Size(min = 10, max = 500) String motivo) { }
