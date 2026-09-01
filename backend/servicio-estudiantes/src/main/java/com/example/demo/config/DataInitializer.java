package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.model.Estudiante;
import com.example.demo.model.FichaMedica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.FichaMedicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.model.DerivacionPsicologica;
import com.example.demo.repository.DerivacionPsicologicaRepository;

@Component
public class DataInitializer implements CommandLineRunner {

	private final UsuarioRepository repo;
	private final PasswordEncoder encoder;
	private final EstudianteRepository estudianteRepository;
	private final FichaMedicaRepository fichaMedicaRepository;
	private final DerivacionPsicologicaRepository derivacionRepository;

	public DataInitializer(UsuarioRepository repo, PasswordEncoder encoder, EstudianteRepository estudianteRepository, FichaMedicaRepository fichaMedicaRepository, DerivacionPsicologicaRepository derivacionRepository) {
		this.repo = repo;
		this.encoder = encoder;
		this.estudianteRepository = estudianteRepository;
		this.fichaMedicaRepository = fichaMedicaRepository;
		this.derivacionRepository = derivacionRepository;
	}

	@Override
	public void run(String... args) {
		// === USUARIOS DEL SISTEMA ===
		if (!repo.existsByUsername("director")) {
			Usuario u = new Usuario();
			u.setUsername("director");
			u.setPassword(encoder.encode("admin123"));
			u.setNombre("Carlos");
			u.setApellido("Quispe Mendoza");
			u.setEmail("director@iephuamanga.edu.pe");
			u.setRol("DIRECTOR");
			u.setEstado("ACTIVO");
			repo.save(u);
			System.out.println(">>> Usuario DIRECTOR creado: director / admin123");
		}

		if (!repo.existsByUsername("docente1")) {
			Usuario u = new Usuario();
			u.setUsername("docente1");
			u.setPassword(encoder.encode("docente123"));
			u.setNombre("Ana");
			u.setApellido("Flores Huanca");
			u.setEmail("ana.flores@iephuamanga.edu.pe");
			u.setRol("DOCENTE");
			u.setEstado("ACTIVO");
			repo.save(u);
			System.out.println(">>> Usuario DOCENTE creado: docente1 / docente123");
		}

		if (!repo.existsByUsername("docente2")) {
			Usuario u = new Usuario();
			u.setUsername("docente2");
			u.setPassword(encoder.encode("docente123"));
			u.setNombre("Luis");
			u.setApellido("Palomino Ccahuana");
			u.setEmail("luis.palomino@iephuamanga.edu.pe");
			u.setRol("DOCENTE");
			u.setEstado("ACTIVO");
			repo.save(u);
		}

		if (!repo.existsByUsername("enfermera1")) {
			Usuario u = new Usuario();
			u.setUsername("enfermera1");
			u.setPassword(encoder.encode("salud123"));
			u.setNombre("María");
			u.setApellido("Ccopa Suni");
			u.setEmail("maria.ccopa@iephuamanga.edu.pe");
			u.setRol("ENFERMERA");
			u.setEstado("ACTIVO");
			repo.save(u);
			System.out.println(">>> Usuario ENFERMERA creado: enfermera1 / salud123");
		}

		if (!repo.existsByUsername("secretaria1")) {
			Usuario u = new Usuario();
			u.setUsername("secretaria1");
			u.setPassword(encoder.encode("admin123"));
			u.setNombre("Rosa");
			u.setApellido("Condori Ayala");
			u.setEmail("rosa.condori@iephuamanga.edu.pe");
			u.setRol("SECRETARIA");
			u.setEstado("ACTIVO");
			repo.save(u);
			System.out.println(">>> Usuario SECRETARIA creado: secretaria1 / admin123");
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
}