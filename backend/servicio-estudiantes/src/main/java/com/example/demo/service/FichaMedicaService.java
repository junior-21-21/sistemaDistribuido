package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.FichaMedicaRequest;
import com.example.demo.model.FichaMedica;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.FichaMedicaRepository;

@Service
@Transactional(readOnly = true)
public class FichaMedicaService {

    private final FichaMedicaRepository repository;
    private final EstudianteRepository estudianteRepository;

    public FichaMedicaService(
            FichaMedicaRepository repository,
            EstudianteRepository estudianteRepository) {
        this.repository = repository;
        this.estudianteRepository = estudianteRepository;
    }

    public List<FichaMedica> listarTodas() {
        return repository.findAll();
    }

    public Optional<FichaMedica> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<FichaMedica> buscarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId);
    }

    @Transactional
    public FichaMedica crear(FichaMedicaRequest request, String username) {
        if (!estudianteRepository.existsById(request.estudianteId())) {
            throw new IllegalArgumentException("Estudiante no encontrado");
        }
        if (repository.existsByEstudianteId(request.estudianteId())) {
            throw new IllegalArgumentException("El estudiante ya tiene una ficha médica");
        }
        FichaMedica ficha = new FichaMedica();
        ficha.setEstudianteId(request.estudianteId());
        actualizarCampos(ficha, request);
        ficha.setUltimaActualizacionPor(username);
        return repository.save(ficha);
    }

    @Transactional
    public Optional<FichaMedica> actualizar(
            Long id,
            FichaMedicaRequest request,
            String username) {
        return repository.findById(id).map(ficha -> {
            if (!ficha.getEstudianteId().equals(request.estudianteId())) {
                throw new IllegalArgumentException("No se puede cambiar el estudiante de una ficha médica");
            }
            actualizarCampos(ficha, request);
            ficha.setUltimaActualizacionPor(username);
            return repository.save(ficha);
        });
    }

    private void actualizarCampos(FichaMedica ficha, FichaMedicaRequest request) {
        ficha.setTipoSangre(request.tipoSangre());
        ficha.setAlergias(limpiar(request.alergias()));
        ficha.setCondicionesCronicas(limpiar(request.condicionesCronicas()));
        ficha.setMedicamentosHabituales(limpiar(request.medicamentosHabituales()));
        ficha.setDiscapacidades(limpiar(request.discapacidades()));
        ficha.setObservacionesGenerales(limpiar(request.observacionesGenerales()));
        ficha.setTipoSeguro(request.tipoSeguro());
        ficha.setNroSeguro(limpiar(request.nroSeguro()));
        ficha.setContactoEmergenciaNombre(limpiar(request.contactoEmergenciaNombre()));
        ficha.setContactoEmergenciaTelefono(limpiar(request.contactoEmergenciaTelefono()));
        ficha.setContactoEmergenciaParentesco(limpiar(request.contactoEmergenciaParentesco()));
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
