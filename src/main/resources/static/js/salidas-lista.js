document.querySelectorAll('form[action$="/pago"]').forEach(form => {
    form.addEventListener('submit', event => {
        const fila = form.closest('tr');
        const placa = fila.querySelector('.vehicle-plate').textContent.trim();
        const importe = fila.querySelector('[data-label="Cuota de agencia"]').textContent.trim();
        const medio = form.querySelector('[name="medioPago"]');
        const descripcion = medio ? medio.selectedOptions[0].textContent : 'Efectivo';
        if (!window.confirm(`¿Confirma que recibió ${importe} del vehículo ${placa} por ${descripcion}? Se registrará en la caja abierta.`)) {
            event.preventDefault();
        }
    });
});
