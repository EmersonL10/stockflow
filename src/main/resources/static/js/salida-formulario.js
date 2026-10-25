(() => {
    const turno = document.getElementById('turno');
    const ruta = document.getElementById('rutaReal');
    if (!turno || !ruta) return;
    const actualizarRuta = () => {
        if (!ruta.value.trim()) ruta.value = turno.selectedOptions[0]?.dataset.ruta || '';
        const opcion = turno.selectedOptions[0];
        let cantidad = 0;
        const idsCarga = document.getElementById('cargaIds');
        if (idsCarga) idsCarga.value = '';
        document.querySelectorAll('.carga-turno').forEach(panel => {
            panel.hidden = panel.dataset.turno !== turno.value;
            if (!panel.hidden) { cantidad = Number(panel.dataset.cantidad); if (idsCarga) idsCarga.value = panel.dataset.ids; }
        });
        const aviso = document.getElementById('sinVehiculoCarga');
        if (aviso) aviso.hidden = Boolean(turno.value);
        const confirmacion = document.getElementById('confirmacionCarga');
        const check = document.getElementById('cargaConfirmada');
        if (confirmacion) confirmacion.hidden = cantidad === 0;
        if (check) check.required = cantidad > 0;
        document.getElementById('resumenTurno').textContent = opcion?.dataset.resumen || '';
        const pasajeros = document.getElementById('cantidadPasajeros');
        if (opcion?.dataset.capacidad) pasajeros.max = opcion.dataset.capacidad;
        else pasajeros.removeAttribute('max');
    };
    turno.addEventListener('change', () => {
        ruta.value = turno.selectedOptions[0]?.dataset.ruta || '';
        const check = document.getElementById('cargaConfirmada');
        if (check) check.checked = false;
        actualizarRuta();
    });
    actualizarRuta();
    const pago = document.getElementById('pagoAlSalir');
    const campoMedio = document.getElementById('campoMedioPago');
    if (pago && campoMedio) {
        const mostrarMedio = () => { campoMedio.hidden = pago.value !== 'true'; };
        pago.addEventListener('change', mostrarMedio);
        mostrarMedio();
    }
})();
