// Backend FALSO para probar la interfaz sin descargar ni instalar nada.
//  - En el navegador: index.html?mock   (añade &installed y &update para ver la pantalla principal / el botón naranja)
//  - En el exe de demo (GitanoLand-Launcher-Demo.exe): se carga solo, junto con demo.js (panel de estados).
(function () {
  const wait = (ms, v) => new Promise((r) => setTimeout(() => r(v), ms));
  const D = (window.DEMO = window.DEMO || {});
  D.update = D.update || (location.search.includes("update") ? "new" : "none");   // none | new | offline
  D.fail = false;
  let cfg = { installed: location.search.includes("installed"), kind: "nopremium", mode: "freesm", profile: "Medio", variant: "java21", ram: 6, nick: "Endo", base: "C:\\Users\\Endo\\AppData\\Local\\GitanoLand", tag: "v1.0.9-java21" };
  D.cfg = () => cfg;
  const w = () => ({ kind: cfg.kind, mode: cfg.mode, profile: cfg.profile, variant: cfg.variant, ram: cfg.ram, nick: cfg.nick, base: cfg.base, ramTouched: true });
  window.MOCK_API = {
    init: () => wait(50, { sys: { ram_total: D.ram || 16, version: "2.0.0-demo", base: cfg.base, frozen: false, installed_here: true }, cfg, w: w() }),
    client_info() {}, minimize() {}, close() {}, open_path() {}, open_url(u) { console.log("url", u); }, open_logs() {},
    pick_folder: () => wait(100, "D:\\Juegos\\GitanoLand"),
    pending_update: () => wait(500, D.update === "offline" ? { installed: "v1.0.9-java21", latest: null }
      : { installed: "v1.0.9-java21", latest: D.update === "new" ? "v1.1.0-java21" : "v1.0.9-java21" }),
    launch: () => wait(300, { ok: true }),
    start_install(ww, up) {
      let i = 0;
      const t = setInterval(() => {
        if (D.fail && i === 2) { clearInterval(t); return GL.event({ type: "fail", message: "No se pudo descargar GitanoLand-1.0.9-java21-Medio.zip (HTTP 503) [simulado]", tail: "[04:20:11] Instalar: freesm Medio java21\n[04:20:12] Leyendo la lista de archivos de la version...\n[04:20:14] ERROR: HTTP 503" }); }
        GL.event({ type: "step", i }); GL.event({ type: "progress", frac: (i + 1) / 5 }); GL.event({ type: "log", text: "Descargando archivo " + (i + 1) + " de 4…" }); i++;
        if (i > 3) { clearInterval(t); setTimeout(() => { cfg = { ...cfg, installed: true, ...ww, tag: "v1.0.9-java21" }; GL.event({ type: "done", cfg }); }, 700); }
      }, 1100);
    },
    guide: () => wait(50, { title: "Guía de TLauncher", steps: [{ t: "Abre TLauncher", b: "Ábrelo como siempre. Cuando veas su ventana principal, sigue." }, { t: "Carpeta del juego", b: "Pulsa el engranaje y pega esta ruta:", copy: "C:\\Users\\Endo\\AppData\\Local\\GitanoLand\\Juego-Medio", open: "x" }, { t: "¡A jugar!", b: "Pulsa JUGAR en TLauncher." }] }),
    login_guide: () => wait(50, { title: "Inicia sesión con tu cuenta", steps: [{ t: "Se abre Prism", b: "Espera a que cargue su ventana." }, { t: "Inicia sesión", b: "Tu cuenta > Gestionar cuentas > Añadir Microsoft." }, { t: "¡A jugar!", b: "Cierra Prism y pulsa Jugar aquí otra vez." }] }),
    delete_instance: () => wait(300, { ok: true, cfg: { ...cfg, installed: false } }),
    make_report: () => wait(50, { text: "informe de prueba", url: "https://github.com/3nndo/GitanoLand/issues/new", path: "x" }),
    install_system: () => wait(300, { ok: true }), skip_system: () => wait(10, true), is_demo: () => true,
  };
})();
