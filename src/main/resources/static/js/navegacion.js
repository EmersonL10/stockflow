(() => {
    const sidebar = document.querySelector('.sidebar');
    const toggle = sidebar?.querySelector('.menu-toggle');
    if (!toggle) return;
    const mobile = window.matchMedia('(max-width: 900px)');
    const ajustar = () => {
        sidebar.classList.toggle('nav-collapsed', mobile.matches);
        toggle.setAttribute('aria-expanded', String(!mobile.matches));
    };
    toggle.addEventListener('click', () => {
        const collapsed = sidebar.classList.toggle('nav-collapsed');
        toggle.setAttribute('aria-expanded', String(!collapsed));
    });
    mobile.addEventListener('change', ajustar);
    ajustar();
    document.querySelectorAll('form[data-confirmar-estado]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirmarEstado)) event.preventDefault();
        });
    });
})();

// Al cambiar el día de Lima, refrescar consultas; conservar formularios en edición.
(() => {
    const dia = document.getElementById('diaOperativo')?.dataset.fecha;
    if (!dia) return;
    let editando = false;
    document.querySelectorAll('form[method="post"]').forEach(form => {
        form.addEventListener('input', () => { editando = true; });
        form.addEventListener('change', () => { editando = true; });
    });
    const hoyLima = () => new Intl.DateTimeFormat('en-CA', {timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    let advertido = false;
    const revisar = () => {
        if (hoyLima() === dia || advertido) return;
        if (!editando && !document.querySelector('main.form-page') && !/\/(nuevo|editar)(\/|$)/.test(location.pathname)) {
            const url = new URL(location.href);
            if (url.pathname === '/salidas' && !url.searchParams.get('fecha') && url.searchParams.get('todasFechas') !== 'true') url.search = '';
            location.replace(url.href);
        } else {
            advertido = true;
            const aviso = document.createElement('p'); aviso.className = 'error-message'; aviso.setAttribute('role','alert');
            aviso.textContent = 'Cambió el día en Pucallpa. Tus datos siguen aquí. Revisa y vuelve a abrir el formulario antes de registrar una nueva operación.';
            document.querySelector('main')?.prepend(aviso);
        }
    };
    setInterval(revisar, 30000);
    window.addEventListener('focus', revisar);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) revisar(); });
})();
