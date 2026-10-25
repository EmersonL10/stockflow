document.querySelectorAll('form[data-confirmar]').forEach(form => {
    form.addEventListener('submit', event => {
        if (!window.confirm(form.dataset.confirmar)) event.preventDefault();
    });
});
document.querySelectorAll('[data-imprimir]').forEach(button => {
    button.addEventListener('click', () => window.print());
});
let detallesImpresion = [];
window.addEventListener('beforeprint', () => {
    detallesImpresion = Array.from(document.querySelectorAll('details.print-expand')).map(element => ({ element, abierto: element.open }));
    detallesImpresion.forEach(({ element }) => { element.open = true; });
});
window.addEventListener('afterprint', () => {
    detallesImpresion.forEach(({ element, abierto }) => { element.open = abierto; });
});
