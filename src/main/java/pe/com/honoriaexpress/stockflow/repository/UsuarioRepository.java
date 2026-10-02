package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}