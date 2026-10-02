package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorUsername(String username);

    Optional<Usuario> buscarPorEmail(String email);

    Usuario guardar(Usuario usuario);

    boolean existePorUsername(String username);

    boolean existePorEmail(String email);
}