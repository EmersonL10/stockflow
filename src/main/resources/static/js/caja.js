(() => {
    const soles = centavos => 'S/ ' + new Intl.NumberFormat('es-PE', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(Number(centavos) / 100);
    const centavos = valor => {
        if (!/^\d+(?:\.\d{1,2})?$/.test(valor)) return null;
        const [entero, decimal = ''] = valor.split('.');
        return BigInt(entero) * 100n + BigInt(decimal.padEnd(2, '0'));
    };
    document.querySelectorAll('[data-cierre-caja]').forEach(form => {
        const contado = form.querySelector('#contado');
        const motivo = form.querySelector('#observacion');
        const campo = form.querySelector('#campoMotivo');
        const resultado = form.querySelector('#resultadoConteo');
        const esperado = centavos(form.dataset.esperado);
        const comparar = () => {
            const efectivo = centavos(contado.value);
            const diferencia = efectivo === null ? null : efectivo - esperado;
            const distinta = diferencia !== null && diferencia !== 0n;
            motivo.required = distinta;
            campo.hidden = !distinta && !motivo.value.trim();
            resultado.textContent = diferencia === null ? 'Escribe tu conteo para compararlo.'
                : diferencia === 0n ? 'El efectivo coincide. Puedes cerrar caja.'
                : diferencia < 0n ? 'Faltan ' + soles(-diferencia) + '. Explica el motivo para cerrar.'
                : 'Sobran ' + soles(diferencia) + '. Explica el motivo para cerrar.';
            resultado.className = 'form-wide ' + (distinta ? 'error-message' : 'field-help');
        };
        contado.addEventListener('input', comparar); comparar();
        form.addEventListener('submit', event => {
            comparar();
            if (!form.reportValidity()) { event.preventDefault(); return; }
            if (!window.confirm('¿Confirma el cierre con ' + soles(centavos(contado.value)) + ' contados? Después no podrá agregar movimientos ni modificar esta caja.')) event.preventDefault();
        });
    });
    document.querySelectorAll('a[href="#cierre"]').forEach(link => link.addEventListener('click', () => {
        const cierre = document.getElementById('cierre'); if (cierre) cierre.open = true;
    }));
    // El servidor también comprueba el día en cada movimiento o cobro.
    const seccion = document.querySelector('[data-movimientos-diarios]');
    const verificarDia = () => {
        if (!seccion || seccion.hidden) return;
        const hoy = new Intl.DateTimeFormat('en-CA', {timeZone: 'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
        if (seccion.dataset.fecha === hoy) return;
        seccion.hidden = true;
        const estado = document.querySelector('[data-estado-caja]');
        if (estado) estado.textContent = 'Pendiente de arqueo · importes en soles';
        const cierre = document.getElementById('cierre'); if (cierre) cierre.open = true;
        const aviso = document.createElement('p'); aviso.className = 'error-message'; aviso.setAttribute('role','status');
        aviso.textContent = 'Terminó el día de esta caja. Ya no recibe movimientos. Cuente el efectivo y confirme el cierre.';
        document.querySelector('.caja-saldo')?.before(aviso);
    };
    setInterval(verificarDia, 30000); window.addEventListener('focus', verificarDia);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) verificarDia(); });
})();
