package com.example.demo.mapper;

import com.example.demo.dto.NotaDTO;
import com.example.demo.model.Nota;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotaMapper {
    NotaDTO toDto(Nota entity);
    Nota toEntity(NotaDTO dto);
}
