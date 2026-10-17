package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Component
@Order(0)
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String directorUsername;
    private final String directorPassword;
    private final String directorEmail;
    private final String systemAdminUsername;
    private final String systemAdminPassword;
    private final String systemAdminEmail;

    public BootstrapAdminInitializer(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:director}") String directorUsername,
            @Value("${app.bootstrap-admin.password:}") String directorPassword,
            @Value("${app.bootstrap-admin.email:director@localhost}") String directorEmail,
            @Value("${app.bootstrap-system-admin.username:admin}") String systemAdminUsername,
            @Value("${app.bootstrap-system-admin.password:}") String systemAdminPassword,
            @Value("${app.bootstrap-system-admin.email:admin@localhost}") String systemAdminEmail) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.directorUsername = directorUsername;
        this.directorPassword = directorPassword;
        this.directorEmail = directorEmail;
        this.systemAdminUsername = systemAdminUsername;
        this.systemAdminPassword = systemAdminPassword;
        this.systemAdminEmail = systemAdminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        createInitialAccount(
                directorUsername, directorPassword, directorEmail,
                "Director", "DIRECTOR", "BOOTSTRAP_ADMIN_PASSWORD");
        createInitialAccount(
                systemAdminUsername, systemAdminPassword, systemAdminEmail,
                "Administrador", "DIRECTOR", "BOOTSTRAP_SYSTEM_ADMIN_PASSWORD");

        if (repository.count() == 0) {
            log.warn("No hay usuarios. Configure las contraseñas iniciales o habilite SEED_DEMO_DATA.");
        }
    }

    private void createInitialAccount(
            String username,
            String password,
            String email,
            String nombre,
            String rol,
            String passwordVariable) {
        if (username == null || username.isBlank() || repository.existsByUsername(username.trim())) {
            return;
        }
        if (password == null || password.isBlank()) {
            // Sin contraseña configurada no se crea la cuenta: el servicio debe arrancar igual.
            log.warn("{} no está configurada; no se crea la cuenta inicial con rol {}.", passwordVariable, rol);
            return;
        }
        if (password.length() < 4) {
            throw new IllegalStateException(
                    passwordVariable + " debe tener al menos 4 caracteres");
        }

        Usuario account = new Usuario();
        account.setUsername(username.trim());
        account.setPassword(passwordEncoder.encode(password));
        account.setNombre(nombre);
        account.setApellido("Inicial");
        account.setEmail(email.trim());
        account.setRol(rol);
        account.setEstado("ACTIVO");
        account.setDebeCambiarClave(true);
        repository.save(account);
        log.info("Usuario inicial con rol {} creado; deberá cambiar su contraseña al ingresar.", rol);
    }
}
