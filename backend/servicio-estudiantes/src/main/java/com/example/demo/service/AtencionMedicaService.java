package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.AtencionMedicaDTO;
import com.example.demo.dto.CrearAtencionMedicaRequest;
import com.example.demo.model.AtencionMedica;
import com.example.demo.model.FichaMedica;
import com.example.demo.repository.AtencionMedicaRepository;
import com.example.demo.repository.FichaMedicaRepository;

@Service
@Transactional(readOnly = true)
public class AtencionMedicaService {

    private final AtencionMedicaRepository repository;
    private final FichaMedicaRepository fichaRepository;
    private final NotificacionClient notificacionClient;

    public AtencionMedicaService(
            AtencionMedicaRepository repository,
            FichaMedicaRepository fichaRepository,
            NotificacionClient notificacionClient) {
        this.repository = repository;
        this.fichaRepository = fichaRepository;
        this.notificacionClient = notificacionClient;
    }

    public List<AtencionMedicaDTO> listarPorFicha(Long fichaId) {
        return repository.findByFichaMedicaIdOrderByFechaAtencionDesc(fichaId).stream()
                .map(AtencionMedicaDTO::from)
                .toList();
    }

    public Page<AtencionMedicaDTO> listarPorFicha(Long fichaId, Pageable pageable) {
        return repository.findByFichaMedicaId(fichaId, pageable).map(AtencionMedicaDTO::from);
    }

    @Transactional
    public AtencionMedicaDTO registrar(CrearAtencionMedicaRequest request, String username) {
        FichaMedica ficha = fichaRepository.findById(request.fichaMedicaId())
                .orElseThrow(() -> new IllegalArgumentException("Ficha médica no encontrada"));
        AtencionMedica atencion = new AtencionMedica();
        atencion.setFichaMedica(ficha);
        atencion.setMotivo(request.motivo().trim());
        atencion.setDiagnostico(request.diagnostico().trim());
        atencion.setTratamiento(request.tratamiento().trim());
        atencion.setRegistradoPor(username);
        String destino = request.destino() == null || request.destino().isBlank() ? "RETORNA_AULA" : request.destino();
        String establecimiento = request.establecimientoSalud() == null ? null : request.establecimientoSalud().trim();
        if ("TRASLADO_SALUD".equals(destino) && (establecimiento == null || establecimiento.isEmpty())) {
            throw new IllegalArgumentException("Indica el establecimiento de salud al que se traslada al estudiante");
        }
        // Retiro o traslado implican haber llamado a la familia.
        boolean avisado = Boolean.TRUE.equals(request.apoderadoAvisado()) || !"RETORNA_AULA".equals(destino);
        atencion.setDestino(destino);
        atencion.setApoderadoAvisado(avisado);
        atencion.setEstablecimientoSalud("TRASLADO_SALUD".equals(destino) ? establecimiento : null);
        atencion.setCubiertoSis("TRASLADO_SALUD".equals(destino) ? request.cubiertoSis() : null);
        AtencionMedica guardada = repository.save(atencion);
        if (avisado) {
            notificacionClient.topico(guardada.getId(), ficha.getEstudianteId(), guardada.getMotivo(),
                    destino, guardada.getEstablecimientoSalud());
        }
        return AtencionMedicaDTO.from(guardada);
    }
}
