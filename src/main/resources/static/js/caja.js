document.querySelectorAll('[data-cierre-caja]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm('¿Confirma el cierre? Después no podrá agregar movimientos ni modificar esta caja.')) {
            event.preventDefault();
        }
    });
});
