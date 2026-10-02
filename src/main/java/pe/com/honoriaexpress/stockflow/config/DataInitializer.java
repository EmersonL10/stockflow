package pe.com.honoriaexpress.stockflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.com.honoriaexpress.stockflow.entity.Rol;
import pe.com.honoriaexpress.stockflow.entity.Usuario;
import pe.com.honoriaexpress.stockflow.repository.RolRepository;
import pe.com.honoriaexpress.stockflow.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) {

        Rol administrador = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseGet(() -> rolRepository.save(
                        Rol.builder()
                                .nombre("ADMINISTRADOR")
                                .descripcion("Acceso total al sistema")
                                .build()
                ));

        rolRepository.findByNombre("OPERATIVO")
                .orElseGet(() -> rolRepository.save(
                        Rol.builder()
                                .nombre("OPERATIVO")
                                .descripcion("Gestión de operaciones de transporte")
                                .build()
                ));

        rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(
                        Rol.builder()
                                .nombre("CAJA")
                                .descripcion("Gestión de caja y movimientos financieros")
                                .build()
                ));

        String adminUsername =
                environment.getProperty("APP_ADMIN_USERNAME", "admin");

        String adminPassword =
                environment.getProperty("APP_ADMIN_PASSWORD");

        String adminEmail =
                environment.getProperty(
                        "APP_ADMIN_EMAIL",
                        "admin@stockflow.local"
                );

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Debe definir la variable de entorno APP_ADMIN_PASSWORD"
            );
        }

        if (!usuarioRepository.existsByUsername(adminUsername)) {

            Usuario usuario = Usuario.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .nombreCompleto("Administrador StockFlow")
                    .email(adminEmail)
                    .activo(true)
                    .rol(administrador)
                    .build();

            usuarioRepository.save(usuario);
        }
    }
}