package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.IngresoDTO;
import com.example.demo.dto.ResumenIngresosDTO;
import com.example.demo.model.Estudiante;
import com.example.demo.model.IngresoColegio;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.IngresoColegioRepository;

/**
 * Control de ingreso en portería: se escanea el código de barras del DNI (o se digita) y se
 * registra la llegada una sola vez por día, marcando tardanza según la hora límite del turno.
 */
@Service
public class IngresoService {

    private static final Pattern DIGITOS = Pattern.compile("\\d+");

    private final IngresoColegioRepository ingresos;
    private final EstudianteRepository estudiantes;
    private final NotificacionClient notificaciones;
    private final ZoneId zona;
    private final LocalTime cambioTurno;
    private final LocalTime limiteManana;
    private final LocalTime limiteTarde;
    private final String notificar;

    public IngresoService(IngresoColegioRepository ingresos, EstudianteRepository estudiantes,
                          NotificacionClient notificaciones,
                          @Value("${ingreso.zona-horaria:America/Lima}") String zona,
                          @Value("${ingreso.cambio-turno:12:30}") String cambioTurno,
                          @Value("${ingreso.limite-manana:07:45}") String limiteManana,
                          @Value("${ingreso.limite-tarde:13:45}") String limiteTarde,
                          @Value("${ingreso.notificar:TARDANZAS}") String notificar) {
        this.ingresos = ingresos;
        this.estudiantes = estudiantes;
        this.notificaciones = notificaciones;
        this.zona = ZoneId.of(zona);
        this.cambioTurno = LocalTime.parse(cambioTurno);
        this.limiteManana = LocalTime.parse(limiteManana);
        this.limiteTarde = LocalTime.parse(limiteTarde);
        this.notificar = notificar.toUpperCase(Locale.ROOT);
    }

    @Transactional
    public IngresoDTO registrar(String codigo, String metodo, String usuario) {
        Estudiante estudiante = buscarPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un estudiante con ese DNI"));
        if (!"ACTIVO".equalsIgnoreCase(estudiante.getEstado())) {
            throw new IllegalArgumentException(estudiante.getNombre() + " " + estudiante.getApellido()
                    + " no está activo en el colegio (estado: " + estudiante.getEstado() + ")");
        }
        LocalDateTime ahora = LocalDateTime.now(zona);
        LocalDate hoy = ahora.toLocalDate();
        Optional<IngresoColegio> previo = ingresos.findByEstudianteIdAndFecha(estudiante.getId(), hoy);
        if (previo.isPresent()) {
            return aDto(previo.get(), estudiante, true);
        }

        LocalTime hora = ahora.toLocalTime().withNano(0);
        String turno = hora.isBefore(cambioTurno) ? "MANANA" : "TARDE";
        LocalTime limite = "MANANA".equals(turno) ? limiteManana : limiteTarde;
        IngresoColegio ingreso = new IngresoColegio();
        ingreso.setEstudianteId(estudiante.getId());
        ingreso.setFecha(hoy);
        ingreso.setHora(hora);
        ingreso.setTurno(turno);
        ingreso.setEstado(hora.isAfter(limite) ? "TARDANZA" : "PUNTUAL");
        ingreso.setMetodo(normalizarMetodo(metodo));
        ingreso.setRegistradoPor(usuario);
        try {
            ingreso = ingresos.saveAndFlush(ingreso);
        } catch (DataIntegrityViolationException carrera) {
            // Dos lectores escanearon al mismo estudiante a la vez: se devuelve el registro existente.
            return ingresos.findByEstudianteIdAndFecha(estudiante.getId(), hoy)
                    .map(existente -> aDto(existente, estudiante, true))
                    .orElseThrow(() -> carrera);
        }

        boolean esTardanza = "TARDANZA".equals(ingreso.getEstado());
        if ("TODOS".equals(notificar) || ("TARDANZAS".equals(notificar) && esTardanza)) {
            notificaciones.ingreso(ingreso.getId(), estudiante.getId(), estudiante.getNombre(), hora, esTardanza);
        }
        return aDto(ingreso, estudiante, false);
    }

    @Transactional(readOnly = true)
    public ResumenIngresosDTO resumenDeHoy() {
        LocalDate hoy = LocalDate.now(zona);
        List<IngresoColegio> delDia = ingresos.findByFechaOrderByHoraDesc(hoy);
        Map<Long, Estudiante> porId = estudiantes.findAllById(delDia.stream().map(IngresoColegio::getEstudianteId).toList())
                .stream().collect(Collectors.toMap(Estudiante::getId, Function.identity()));
        List<IngresoDTO> lista = delDia.stream()
                .filter(i -> porId.containsKey(i.getEstudianteId()))
                .map(i -> aDto(i, porId.get(i.getEstudianteId()), false))
                .toList();
        long tardanzas = lista.stream().filter(i -> "TARDANZA".equals(i.estado())).count();
        return new ResumenIngresosDTO(hoy, estudiantes.countByEstado("ACTIVO"), lista.size(),
                lista.size() - tardanzas, tardanzas, lista);
    }

    @Transactional(readOnly = true)
    public List<IngresoDTO> historialEstudiante(Long estudianteId, LocalDate desde, LocalDate hasta) {
        Estudiante estudiante = estudiantes.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        return ingresos.findByEstudianteIdInAndFechaBetween(List.of(estudianteId), desde, hasta).stream()
                .sorted(Comparator.comparing(IngresoColegio::getFecha).reversed())
                .map(i -> aDto(i, estudiante, false))
                .toList();
    }

    /**
     * El código de barras del DNI trae el número del documento; algunos lectores agregan el
     * dígito verificador u otros datos. Se prueba el número completo y luego los primeros 8 dígitos.
     */
    Optional<Estudiante> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        Matcher m = DIGITOS.matcher(codigo);
        while (m.find()) {
            String digitos = m.group();
            if (digitos.length() < 8) continue;
            Optional<Estudiante> exacto = estudiantes.findByDni(digitos);
            if (exacto.isPresent()) return exacto;
            Optional<Estudiante> primeros = estudiantes.findByDni(digitos.substring(0, 8));
            if (primeros.isPresent()) return primeros;
        }
        return Optional.empty();
    }

    private IngresoDTO aDto(IngresoColegio i, Estudiante e, boolean yaRegistrado) {
        LocalTime limite = "MANANA".equals(i.getTurno()) ? limiteManana : limiteTarde;
        long minutosTarde = Math.max(0, Duration.between(limite, i.getHora()).toMinutes());
        String dni = e.getDni() == null ? "" : e.getDni();
        String dniFinal = dni.length() > 3 ? "•••••" + dni.substring(dni.length() - 3) : dni;
        return new IngresoDTO(i.getId(), e.getId(), e.getNombre(), e.getApellido(), dniFinal, e.getGradoId(),
                e.getSeccionId(), i.getFecha(), i.getHora(), i.getTurno(), i.getEstado(),
                "TARDANZA".equals(i.getEstado()) ? minutosTarde : 0, i.getMetodo(), yaRegistrado);
    }

    private String normalizarMetodo(String metodo) {
        String valor = metodo == null ? "" : metodo.trim().toUpperCase(Locale.ROOT);
        return switch (valor) {
            case "CAMARA", "MANUAL" -> valor;
            default -> "ESCANER";
        };
    }
}
