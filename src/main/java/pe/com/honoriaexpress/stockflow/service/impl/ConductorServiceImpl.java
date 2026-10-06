package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.Conductor;
import pe.com.honoriaexpress.stockflow.repository.ConductorRepository;
import pe.com.honoriaexpress.stockflow.service.ConductorService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConductorServiceImpl implements ConductorService {

    private final ConductorRepository conductorRepository;


    @Override
    public List<Conductor> listarTodos() {
        return conductorRepository.findAll();
    }


    @Override
    public Optional<Conductor> buscarPorId(Long id) {
        return conductorRepository.findById(id);
    }


    @Override
    public Conductor guardar(Conductor conductor) {
        return conductorRepository.save(conductor);
    }


    @Override
    public boolean existePorDni(String dni) {
        return conductorRepository.existsByDni(dni);
    }


    @Override
    public boolean existePorLicencia(String licencia) {
        return conductorRepository.existsByLicencia(licencia);
    }


    @Override
    public boolean existeDniEnOtroConductor(String dni, Long id) {
        return conductorRepository.existsByDniAndIdNot(dni, id);
    }


    @Override
    public boolean existeLicenciaEnOtroConductor(String licencia, Long id) {
        return conductorRepository.existsByLicenciaAndIdNot(licencia, id);
    }


    @Override
    public void cambiarEstado(Long id) {

        Conductor conductor = conductorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conductor no encontrado con id: " + id
                        )
                );

        conductor.setActivo(!conductor.getActivo());

        conductorRepository.save(conductor);
    }
}