package com.example.demo.mapper;

import com.example.demo.dto.CursoDTO;
import com.example.demo.model.Curso;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CursoMapper {
    CursoDTO toDto(Curso curso);
    Curso toEntity(CursoDTO cursoDTO);
    void updateEntityFromDto(CursoDTO dto, @MappingTarget Curso entity);
}
