document.querySelectorAll('form[action$="/pago"]').forEach(form => {
    form.addEventListener('submit', event => {
        const fila = form.closest('tr');
        const placa = fila.querySelector('[data-label="Vehículo"]').textContent.trim();
        const importe = fila.querySelector('[data-label="Cuota de agencia"]').textContent.trim();
        if (!window.confirm(`¿Confirma que recibió ${importe} del vehículo ${placa}?`)) {
            event.preventDefault();
        }
    });
});
