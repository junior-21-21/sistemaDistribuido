package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);

    @Query("select distinct u from Usuario u left join u.roles r where " +
           "(:q = '' or lower(u.nombre) like concat('%', :q, '%') or lower(u.apellido) like concat('%', :q, '%') " +
           "or lower(u.username) like concat('%', :q, '%') or lower(u.email) like concat('%', :q, '%')) " +
           "and (:rol = '' or r = :rol) and (:estado = '' or u.estado = :estado)")
    Page<Usuario> buscar(@Param("q") String q, @Param("rol") String rol,
                         @Param("estado") String estado, Pageable pageable);

    @Query("select count(distinct u) from Usuario u join u.roles r where r = :rol and u.estado = 'ACTIVO'")
    long contarActivosConRol(@Param("rol") String rol);
}
