package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.Rol;

import java.util.List;
import java.util.Optional;

public interface RolService {

    List<Rol> listarTodos();

    Optional<Rol> buscarPorId(Long id);

    Optional<Rol> buscarPorNombre(String nombre);

    Rol guardar(Rol rol);

    boolean existePorNombre(String nombre);
}