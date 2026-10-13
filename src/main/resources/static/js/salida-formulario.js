(() => {
    const turno = document.getElementById('turno');
    const ruta = document.getElementById('rutaReal');
    if (!turno || !ruta) return;
    const actualizarRuta = () => {
        if (!ruta.value.trim()) ruta.value = turno.selectedOptions[0]?.dataset.ruta || '';
        const opcion = turno.selectedOptions[0];
        document.getElementById('resumenTurno').textContent = opcion?.dataset.resumen || '';
        const pasajeros = document.getElementById('cantidadPasajeros');
        if (opcion?.dataset.capacidad) pasajeros.max = opcion.dataset.capacidad;
        else pasajeros.removeAttribute('max');
    };
    turno.addEventListener('change', actualizarRuta);
    actualizarRuta();
})();
