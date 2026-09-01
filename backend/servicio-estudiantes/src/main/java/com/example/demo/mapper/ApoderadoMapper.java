package com.example.demo.mapper;

import com.example.demo.dto.ApoderadoDTO;
import com.example.demo.model.Apoderado;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ApoderadoMapper {
    ApoderadoMapper INSTANCE = Mappers.getMapper(ApoderadoMapper.class);

    ApoderadoDTO toDto(Apoderado apoderado);
    Apoderado toEntity(ApoderadoDTO apoderadoDTO);
    void updateEntityFromDto(ApoderadoDTO dto, @MappingTarget Apoderado entity);
}
