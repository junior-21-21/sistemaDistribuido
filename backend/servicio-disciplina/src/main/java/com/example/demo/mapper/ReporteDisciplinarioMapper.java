package com.example.demo.mapper;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.model.ReporteDisciplinario;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReporteDisciplinarioMapper {
    ReporteDisciplinarioDTO toDto(ReporteDisciplinario entity);
    ReporteDisciplinario toEntity(ReporteDisciplinarioDTO dto);
}
