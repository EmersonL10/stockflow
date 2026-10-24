package pe.com.honoriaexpress.stockflow.service;

import org.springframework.data.domain.Page;
import pe.com.honoriaexpress.stockflow.dto.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import java.util.List;

public interface EncomiendaService {
    Page<Encomienda> consultar(String codigo, EstadoEncomienda estado, EstadoPagoEncomienda pago, int pagina);
    Encomienda buscar(Long id);
    List<EncomiendaEvento> eventos(Long id);
    List<Salida> salidasDisponibles(String ruta);
    Encomienda registrar(EncomiendaFormulario datos);
    void editar(Long id, EncomiendaFormulario datos);
    void cobrar(Long id, MedioPago medio);
    void despachar(Long id, Long salidaId);
    void recibirDestino(Long id);
    void entregar(Long id, EntregaEncomiendaFormulario datos);
    void cancelar(Long id, String motivo);
}
