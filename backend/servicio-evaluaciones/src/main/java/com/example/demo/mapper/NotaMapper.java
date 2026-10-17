package com.example.demo.mapper;

import com.example.demo.dto.NotaDTO;
import com.example.demo.model.Nota;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotaMapper {
    NotaDTO toDto(Nota entity);
    Nota toEntity(NotaDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    void updateEntity(NotaDTO dto, @MappingTarget Nota entity);
}
