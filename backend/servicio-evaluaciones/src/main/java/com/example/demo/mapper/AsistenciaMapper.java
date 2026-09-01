package com.example.demo.mapper;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.model.Asistencia;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AsistenciaMapper {
    AsistenciaDTO toDto(Asistencia entity);
    Asistencia toEntity(AsistenciaDTO dto);
}
