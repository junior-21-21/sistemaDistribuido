package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisarPermisoRequest(@NotNull Boolean aprobar, @Size(max = 300) String observacion) {
}
