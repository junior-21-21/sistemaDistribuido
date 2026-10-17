package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import com.example.demo.model.Estudiante;
import com.example.demo.model.FichaMedica;
import com.example.demo.model.Usuario;
import com.example.demo.model.Apoderado;
import com.example.demo.model.ApoderadoEstudiante;
import com.example.demo.model.AtencionMedica;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.FichaMedicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.ApoderadoRepository;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.AtencionMedicaRepository;
import com.example.demo.model.DerivacionPsicologica;
import com.example.demo.repository.DerivacionPsicologicaRepository;
import com.example.demo.model.ModuloInstitucion;
import com.example.demo.repository.ModuloInstitucionRepository;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

	private final UsuarioRepository repo;
	private final PasswordEncoder encoder;
	private final EstudianteRepository estudianteRepository;
	private final FichaMedicaRepository fichaMedicaRepository;
	private final DerivacionPsicologicaRepository derivacionRepository;
	private final ApoderadoRepository apoderadoRepository;
	private final ApoderadoEstudianteRepository vinculoRepository;
	private final AtencionMedicaRepository atencionRepository;
	private final ModuloInstitucionRepository moduloRepository;

	public DataInitializer(UsuarioRepository repo, PasswordEncoder encoder, EstudianteRepository estudianteRepository,
			FichaMedicaRepository fichaMedicaRepository, DerivacionPsicologicaRepository derivacionRepository,
			ApoderadoRepository apoderadoRepository, ApoderadoEstudianteRepository vinculoRepository,
			AtencionMedicaRepository atencionRepository, ModuloInstitucionRepository moduloRepository) {
		this.repo = repo;
		this.encoder = encoder;
		this.estudianteRepository = estudianteRepository;
		this.fichaMedicaRepository = fichaMedicaRepository;
		this.derivacionRepository = derivacionRepository;
		this.apoderadoRepository = apoderadoRepository;
		this.vinculoRepository = vinculoRepository;
		this.atencionRepository = atencionRepository;
		this.moduloRepository = moduloRepository;
	}

	@Override
	public void run(String... args) {
		// === USUARIOS DEL SISTEMA ===
		if (!repo.existsByUsername("director")) {
			Usuario u = new Usuario();
			u.setUsername("director");
			u.setPassword(encoder.encode("DemoDirector2026*"));
			u.setNombre("Carlos");
			u.setApellido("Quispe Mendoza");
			u.setEmail("director@iephuamanga.edu.pe");
			u.setRol("DIRECTOR");
			u.setEstado("ACTIVO");
			u.setDebeCambiarClave(true);
			repo.save(u);
			System.out.println(">>> Usuario DIRECTOR de demostración creado.");
		}

		if (!repo.existsByUsername("docente1")) {
			Usuario u = new Usuario();
			u.setUsername("docente1");
			u.setPassword(encoder.encode("DemoDocente2026*"));
			u.setNombre("Ana");
			u.setApellido("Flores Huanca");
			u.setEmail("ana.flores@iephuamanga.edu.pe");
			u.setRol("DOCENTE");
			u.setEstado("ACTIVO");
			u.setDebeCambiarClave(true);
			repo.save(u);
			System.out.println(">>> Usuario DOCENTE de demostración creado.");
		}

		if (!repo.existsByUsername("docente2")) {
			Usuario u = new Usuario();
			u.setUsername("docente2");
			u.setPassword(encoder.encode("DemoDocente2026*"));
			u.setNombre("Luis");
			u.setApellido("Palomino Ccahuana");
			u.setEmail("luis.palomino@iephuamanga.edu.pe");
			u.setRol("DOCENTE");
			u.setEstado("ACTIVO");
			u.setDebeCambiarClave(true);
			repo.save(u);
		}

		if (!repo.existsByUsername("enfermera1")) {
			Usuario u = new Usuario();
			u.setUsername("enfermera1");
			u.setPassword(encoder.encode("DemoSalud2026*"));
			u.setNombre("María");
			u.setApellido("Ccopa Suni");
			u.setEmail("maria.ccopa@iephuamanga.edu.pe");
			u.setRol("ENFERMERA");
			u.setEstado("ACTIVO");
			u.setDebeCambiarClave(true);
			repo.save(u);
			System.out.println(">>> Usuario ENFERMERA de demostración creado.");
		}

		if (!repo.existsByUsername("secretaria1")) {
			Usuario u = new Usuario();
			u.setUsername("secretaria1");
			u.setPassword(encoder.encode("DemoSecretaria2026*"));
			u.setNombre("Rosa");
			u.setApellido("Condori Ayala");
			u.setEmail("rosa.condori@iephuamanga.edu.pe");
			u.setRol("SECRETARIA");
			u.setEstado("ACTIVO");
			u.setDebeCambiarClave(true);
			repo.save(u);
			System.out.println(">>> Usuario SECRETARIA de demostración creado.");
		}

		crearUsuarioDemo("admin", "DemoAdmin2026*", "Diego", "Salazar Vega",
				"admin@iephuamanga.edu.pe", null, "DIRECTOR");
		crearUsuarioDemo("coord.academica", "DemoCoordinacion2026*", "Elena", "Soto Gutiérrez",
				"elena.soto@iephuamanga.edu.pe", null, "SUBDIRECTOR", "DOCENTE");
		crearUsuarioDemo("coord.convivencia", "DemoCoordinacion2026*", "Marco", "Yupanqui León",
				"marco.yupanqui@iephuamanga.edu.pe", null, "AUXILIAR", "RESPONSABLE_CONVIVENCIA");
		crearUsuarioDemo("auxiliar1", "DemoAuxiliar2026*", "Julia", "Cárdenas Rojas",
				"julia.cardenas@iephuamanga.edu.pe", null, "AUXILIAR");
		crearUsuarioDemo("psicologa1", "DemoPsicologia2026*", "Patricia", "Mendoza Luna",
				"patricia.mendoza@iephuamanga.edu.pe", null, "PSICOLOGO");
		crearUsuarioDemo("mantenimiento1", "DemoMantenimiento2026*", "Oscar", "Poma Huamán",
				"oscar.poma@iephuamanga.edu.pe", null, "MANTENIMIENTO");
		crearUsuarioDemo("portero1", "DemoPorteria2026*", "Teodoro", "Quispe Ccahuana",
				"teodoro.quispe@iephuamanga.edu.pe", null, "PORTERO");
		// El colegio de demostración cuenta con psicóloga, enfermera, mantenimiento y portería.
		for (String modulo : List.of("PSICOLOGIA", "ENFERMERIA", "MANTENIMIENTO", "PORTERIA")) {
			ModuloInstitucion item = moduloRepository.findById(modulo).orElseGet(ModuloInstitucion::new);
			if (item.getCodigo() == null || !item.isHabilitado()) {
				item.setCodigo(modulo);
				item.setHabilitado(true);
				item.setActualizadoPor("datos-demo");
				item.setActualizadoEn(java.time.LocalDateTime.now());
				moduloRepository.save(item);
			}
		}

		// === ESTUDIANTES DE PRUEBA (Huamanga, Ayacucho) ===
		if (estudianteRepository.count() == 0) {
			String[][] datos = {
				{"Rodrigo", "Quispe Sullca", "74521003", "rodrigo.quispe@student.iep.edu.pe", "966123001", "1", "1"},
				{"Valentina", "Huaman Flores", "74521004", "valentina.huaman@student.iep.edu.pe", "966123002", "1", "1"},
				{"Sebastian", "Ccahuana Puma", "74521005", "sebastian.ccahuana@student.iep.edu.pe", "966123003", "1", "2"},
				{"Gabriela", "Palomino Ayala", "74521006", "gabriela.palomino@student.iep.edu.pe", "966123004", "2", "1"},
				{"Diego", "Condori Cardenas", "74521007", "diego.condori@student.iep.edu.pe", "966123005", "2", "1"},
				{"Sofia", "Mendoza Quispe", "74521008", "sofia.mendoza@student.iep.edu.pe", "966123006", "2", "2"},
				{"Mateo", "Huanca Ccopa", "74521009", "mateo.huanca@student.iep.edu.pe", "966123007", "3", "1"},
				{"Isabella", "Sulca Vargas", "74521010", "isabella.sulca@student.iep.edu.pe", "966123008", "3", "1"},
				{"Alejandro", "Mamani Castro", "74521011", "alejandro.mamani@student.iep.edu.pe", "966123009", "3", "2"},
				{"Camila", "Rojas Suni", "74521012", "camila.rojas@student.iep.edu.pe", "966123010", "4", "1"},
				{"Daniel", "Torres Huanca", "74521013", "daniel.torres@student.iep.edu.pe", "966123011", "4", "1"},
				{"Lucia", "Puma Ccahuana", "74521014", "lucia.puma@student.iep.edu.pe", "966123012", "4", "2"},
				{"Fernanda", "Cardenas Quispe", "74521015", "fernanda.cardenas@student.iep.edu.pe", "966123013", "5", "1"},
				{"Miguel", "Vargas Condori", "74521016", "miguel.vargas@student.iep.edu.pe", "966123014", "5", "1"},
				{"Valeria", "Castro Flores", "74521017", "valeria.castro@student.iep.edu.pe", "966123015", "5", "2"},
				{"Andres", "Suni Mamani", "74521018", "andres.suni@student.iep.edu.pe", "966123016", "1", "1"},
				{"Natalia", "Ayala Rojas", "74521019", "natalia.ayala@student.iep.edu.pe", "966123017", "2", "1"},
				{"Emilio", "Huaman Torres", "74521020", "emilio.huaman@student.iep.edu.pe", "966123018", "3", "1"},
				{"Daniela", "Palacios Quispe", "74521021", "daniela.palacios@student.iep.edu.pe", "966123019", "4", "2"},
				{"Roberto", "Quispe Mamani", "74521022", "roberto.qm@student.iep.edu.pe", "966123020", "5", "1"}
			};

			for (String[] d : datos) {
				Estudiante e = new Estudiante();
				e.setNombre(d[0]);
				e.setApellido(d[1]);
				e.setDni(d[2]);
				e.setEmail(d[3]);
				e.setTelefono(d[4]);
				e.setGradoId(Long.parseLong(d[5]));
				e.setSeccionId(Long.parseLong(d[6]));
				e.setEstado("ACTIVO");
				estudianteRepository.save(e);
			}
			System.out.println(">>> 20 Estudiantes de prueba insertados correctamente.");
		}

		// === APODERADOS, VÍNCULOS Y CUENTAS DEL PORTAL ===
		String[][] apoderadosDemo = {
			{"Carmen", "Sullca Rojas", "43110001", "carmen.sullca@familia.pe", "966201001"},
			{"Jorge", "Huamán Pérez", "43110002", "jorge.huaman@familia.pe", "966201002"},
			{"Lucía", "Puma Torres", "43110003", "lucia.puma@familia.pe", "966201003"},
			{"Raúl", "Palomino Soto", "43110004", "raul.palomino@familia.pe", "966201004"},
			{"Ana", "Condori Vega", "43110005", "ana.condori@familia.pe", "966201005"},
			{"Mónica", "Quispe Ayala", "43110006", "monica.quispe@familia.pe", "966201006"},
			{"Pedro", "Huanca Ccopa", "43110007", "pedro.huanca@familia.pe", "966201007"},
			{"Rosa", "Mamani Castro", "43110008", "rosa.mamani@familia.pe", "966201008"},
			{"Víctor", "Rojas Suni", "43110009", "victor.rojas@familia.pe", "966201009"},
			{"Teresa", "Vargas Condori", "43110010", "teresa.vargas@familia.pe", "966201010"}
		};
		for (int i = 0; i < apoderadosDemo.length; i++) {
			String[] data = apoderadosDemo[i];
			Apoderado apoderado = apoderadoRepository.findByDni(data[2]).orElseGet(() -> {
				Apoderado nuevo = new Apoderado();
				nuevo.setNombre(data[0]); nuevo.setApellido(data[1]); nuevo.setDni(data[2]);
				nuevo.setEmail(data[3]); nuevo.setTelefono(data[4]);
				return apoderadoRepository.save(nuevo);
			});
			for (long estudianteId : new long[] {i * 2L + 1, i * 2L + 2}) {
				if (vinculoRepository.findByApoderadoIdAndEstudianteId(apoderado.getId(), estudianteId).isEmpty()) {
					Estudiante estudiante = estudianteRepository.findById(estudianteId).orElse(null);
					if (estudiante != null) {
						ApoderadoEstudiante vinculo = new ApoderadoEstudiante();
						vinculo.setApoderado(apoderado); vinculo.setEstudiante(estudiante);
						vinculo.setParentesco(i % 2 == 0 ? "MADRE" : "PADRE"); vinculo.setPermisoRetiro(true);
						vinculoRepository.save(vinculo);
					}
				}
			}
		}
		List<Apoderado> apoderados = apoderadoRepository.findAll();
		if (!apoderados.isEmpty()) crearUsuarioDemo("apoderado1", "DemoApoderado2026*", "Carmen", "Sullca Rojas",
				"carmen.portal@familia.pe", apoderados.get(0), "APODERADO");
		if (apoderados.size() > 1) crearUsuarioDemo("apoderado2", "DemoApoderado2026*", "Jorge", "Huamán Pérez",
				"jorge.portal@familia.pe", apoderados.get(1), "APODERADO");

		// === FICHAS MÉDICAS DE PRUEBA ===
		if (fichaMedicaRepository.count() == 0) {
			// Fichas para estudiantes con IDs 1–5 (aproximados según orden de inserción)
			String[][] fichas = {
				// tipoSangre, alergias, condiciones, seguro, contactoNombre, contactoTel, parentesco
				{"O+",  "Ninguna",         "Ninguna",              "SIS",     "Carmen Sullca",   "966201001", "Madre"},
				{"A+",  "Polen, polvo",    "Asma leve",            "SIS",     "Jorge Huaman",    "966201002", "Padre"},
				{"B+",  "Ninguna",         "Ninguna",              "SIS",     "Lucia Puma",      "966201003", "Madre"},
				{"AB+", "Penicilina",      "Rinitis alergica",     "ESSALUD", "Raul Palomino",   "966201004", "Padre"},
				{"O-",  "Mariscos",        "Ninguna",              "SIS",     "Ana Condori",     "966201005", "Madre"}
			};

			long[] estudianteIds = {1L, 2L, 3L, 4L, 5L};
			for (int i = 0; i < fichas.length; i++) {
				if (!fichaMedicaRepository.existsByEstudianteId(estudianteIds[i])) {
					String[] f = fichas[i];
					FichaMedica fm = new FichaMedica();
					fm.setEstudianteId(estudianteIds[i]);
					fm.setTipoSangre(f[0]);
					fm.setAlergias(f[1]);
					fm.setCondicionesCronicas(f[2]);
					fm.setTipoSeguro(f[3]);
					fm.setContactoEmergenciaNombre(f[4]);
					fm.setContactoEmergenciaTelefono(f[5]);
					fm.setContactoEmergenciaParentesco(f[6]);
					fm.setUltimaActualizacionPor("enfermera1");
					fichaMedicaRepository.save(fm);
				}
			}
			System.out.println(">>> Fichas médicas de prueba insertadas.");
		}

		if (atencionRepository.count() == 0) {
			String[][] atenciones = {
				{"Dolor de cabeza", "Cefalea tensional leve", "Reposo, hidratación y control en 30 minutos"},
				{"Dificultad respiratoria", "Crisis asmática leve", "Inhalador indicado y aviso al apoderado"},
				{"Golpe durante recreo", "Contusión superficial en rodilla", "Limpieza, hielo local y observación"},
				{"Congestión nasal", "Rinitis alérgica", "Lavado nasal y seguimiento en domicilio"},
				{"Dolor abdominal", "Malestar digestivo sin signos de alarma", "Reposo y comunicación con la familia"}
			};
			for (int i = 0; i < atenciones.length; i++) {
				FichaMedica ficha = fichaMedicaRepository.findByEstudianteId(i + 1L).orElse(null);
				if (ficha != null) {
					AtencionMedica atencion = new AtencionMedica();
					atencion.setFichaMedica(ficha); atencion.setMotivo(atenciones[i][0]);
					atencion.setDiagnostico(atenciones[i][1]); atencion.setTratamiento(atenciones[i][2]);
					atencion.setFechaAtencion(LocalDateTime.now().minusDays(i + 1L));
					atencion.setRegistradoPor("enfermera1"); atencionRepository.save(atencion);
				}
			}
		}

		// === DERIVACIONES PSICOLÓGICAS DE PRUEBA ===
		if (derivacionRepository.count() == 0) {
			DerivacionPsicologica d1 = new DerivacionPsicologica();
			d1.setEstudianteId(1L);
			d1.setDocenteDerivadorId(2L); // docente1
			d1.setMotivo("RENDIMIENTO");
			d1.setDescripcion("El alumno ha mostrado una caída drástica en sus calificaciones y se muestra aislado.");
			d1.setPrioridad("NORMAL");
			d1.setEstado("RECIBIDO");
			
			DerivacionPsicologica d2 = new DerivacionPsicologica();
			d2.setEstudianteId(3L);
			d2.setDocenteDerivadorId(2L);
			d2.setMotivo("BULLYING");
			d2.setDescripcion("Sospecha de acoso por parte de compañeros mayores en el recreo.");
			d2.setPrioridad("URGENTE");
			d2.setEstado("EN_EVALUACION");
			d2.setNotasPsicologo("Entrevista inicial programada con los padres.");

			derivacionRepository.save(d1);
			derivacionRepository.save(d2);
			System.out.println(">>> Derivaciones psicológicas insertadas.");
		}
	}

	private void crearUsuarioDemo(String username, String password, String nombre, String apellido,
			String email, Apoderado apoderado, String... roles) {
		Usuario usuario = repo.findByUsername(username).orElseGet(Usuario::new);
		if (usuario.getId() == null) {
			usuario.setUsername(username); usuario.setPassword(encoder.encode(password));
			usuario.setNombre(nombre); usuario.setApellido(apellido); usuario.setEmail(email);
			usuario.setEstado("ACTIVO"); usuario.setDebeCambiarClave(true);
		}
		LinkedHashSet<String> rolesActuales = new LinkedHashSet<>(usuario.getRoles());
		rolesActuales.removeAll(List.of("COORDINADOR_ACADEMICO", "COORDINADOR_CONVIVENCIA", "ADMIN_SISTEMA"));
		// La coordinación académica pasó a ser Subdirección (ya no tiene el rol de Director).
		if ("coord.academica".equals(username)) rolesActuales.remove("DIRECTOR");
		rolesActuales.addAll(List.of(roles));
		usuario.setRol(roles[0]); usuario.setRoles(rolesActuales);
		if (apoderado != null) usuario.setApoderado(apoderado);
		repo.save(usuario);
	}
}
