package com.example.demo.controller;

import com.example.demo.model.FichaMedica;
import com.example.demo.repository.FichaMedicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FichaMedicaController — Solo accesible por ENFERMERA y DIRECTOR.
 * Gestiona las fichas médicas de los estudiantes.
 */
@RestController
@RequestMapping("/api/fichas-medicas")
public class FichaMedicaController {

    @Autowired
    private FichaMedicaRepository fichaMedicaRepository;

    @GetMapping
    public ResponseEntity<List<FichaMedica>> listarTodas() {
        return ResponseEntity.ok(fichaMedicaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FichaMedica> obtenerPorId(@PathVariable Long id) {
        return fichaMedicaRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<FichaMedica> obtenerPorEstudiante(@PathVariable Long estudianteId) {
        return fichaMedicaRepository.findByEstudianteId(estudianteId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FichaMedica> crear(@RequestBody FichaMedica fichaMedica) {
        if (fichaMedicaRepository.existsByEstudianteId(fichaMedica.getEstudianteId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return new ResponseEntity<>(fichaMedicaRepository.save(fichaMedica), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FichaMedica> actualizar(@PathVariable Long id,
                                                   @RequestBody FichaMedica datos,
                                                   @RequestHeader(value = "X-Username", required = false) String username) {
        return fichaMedicaRepository.findById(id)
            .map(ficha -> {
                ficha.setTipoSangre(datos.getTipoSangre());
                ficha.setAlergias(datos.getAlergias());
                ficha.setCondicionesCronicas(datos.getCondicionesCronicas());
                ficha.setMedicamentosHabituales(datos.getMedicamentosHabituales());
                ficha.setDiscapacidades(datos.getDiscapacidades());
                ficha.setObservacionesGenerales(datos.getObservacionesGenerales());
                ficha.setTipoSeguro(datos.getTipoSeguro());
                ficha.setNroSeguro(datos.getNroSeguro());
                ficha.setContactoEmergenciaNombre(datos.getContactoEmergenciaNombre());
                ficha.setContactoEmergenciaTelefono(datos.getContactoEmergenciaTelefono());
                ficha.setContactoEmergenciaParentesco(datos.getContactoEmergenciaParentesco());
                if (username != null) ficha.setUltimaActualizacionPor(username);
                return ResponseEntity.ok(fichaMedicaRepository.save(ficha));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
