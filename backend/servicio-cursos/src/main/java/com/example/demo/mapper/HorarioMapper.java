package com.example.demo.mapper;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.model.Horario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HorarioMapper {

    @Mapping(source = "curso.id", target = "cursoId")
    @Mapping(source = "curso.nombre", target = "cursoNombre")
    @Mapping(source = "docente.id", target = "docenteId")
    @Mapping(source = "docente.nombre", target = "docenteNombre")
    HorarioDTO toDto(Horario horario);

    @Mapping(source = "cursoId", target = "curso.id")
    @Mapping(source = "docenteId", target = "docente.id")
    Horario toEntity(HorarioDTO horarioDTO);

    @Mapping(source = "cursoId", target = "curso.id")
    @Mapping(source = "docenteId", target = "docente.id")
    void updateEntityFromDto(HorarioDTO dto, @MappingTarget Horario entity);
}
