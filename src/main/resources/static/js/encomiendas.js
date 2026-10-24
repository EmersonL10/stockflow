document.querySelectorAll('form[data-confirmar]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm(form.dataset.confirmar)) event.preventDefault();
    });
});
document.querySelectorAll('[data-imprimir]').forEach(button => {
    button.addEventListener('click', () => window.print());
});
