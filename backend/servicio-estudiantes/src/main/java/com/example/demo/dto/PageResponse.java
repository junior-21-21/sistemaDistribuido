package com.example.demo.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Contrato HTTP estable para resultados paginados.
 * Evita exponer PageImpl, un detalle interno de Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
