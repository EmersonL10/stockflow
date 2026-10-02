package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.Rol;
import pe.com.honoriaexpress.stockflow.repository.RolRepository;
import pe.com.honoriaexpress.stockflow.service.RolService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    @Override
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    @Override
    public Optional<Rol> buscarPorId(Long id) {
        return rolRepository.findById(id);
    }

    @Override
    public Optional<Rol> buscarPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre);
    }

    @Override
    public Rol guardar(Rol rol) {
        return rolRepository.save(rol);
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return rolRepository.existsByNombre(nombre);
    }
}