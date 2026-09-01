package com.example.demo.mapper;

import com.example.demo.dto.DocenteDTO;
import com.example.demo.model.Docente;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DocenteMapper {
    DocenteDTO toDto(Docente docente);
    Docente toEntity(DocenteDTO docenteDTO);
    void updateEntityFromDto(DocenteDTO dto, @MappingTarget Docente entity);
}
