package com.example.demo.mapper;

import com.example.demo.dto.EstudianteDTO;
import com.example.demo.model.Estudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EstudianteMapper {

    @Mapping(target = "gradoNombre", ignore = true)
    @Mapping(target = "seccionNombre", ignore = true)
    EstudianteDTO toDto(Estudiante estudiante);

    Estudiante toEntity(EstudianteDTO estudianteDTO);

    void updateEntityFromDto(EstudianteDTO dto, @MappingTarget Estudiante entity);
}
