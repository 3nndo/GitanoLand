// Panel de estados del exe de DEMO (privado): salta a cualquier pantalla sin descargar ni instalar nada.
(function () {
  const D = window.DEMO;
  const base = "C:\\Users\\Endo\\AppData\\Local\\GitanoLand";
  const home = (update) => () => { D.update = update; S.cfg = { ...D.cfg(), installed: true }; S.w = { kind: "nopremium", mode: "freesm", profile: "Medio", variant: "java21", ram: 6, nick: "Endo", base, ramTouched: true }; show("home"); };
  const wizard = (kind, mode, step) => () => { newW(kind, mode); S.w.nick = "Endo"; show("wizard" + step); };
  const GROUPS = [
    ["Inicio", [
      ["Bienvenida", () => show("welcome")],
      ["Otro launcher", () => show("other")],
      ["Instalar en el PC", () => show("system")],
      ["Actualizando launcher", () => { show("selfupdate"); let f = 0; const t = setInterval(() => { f += 0.04; GL.event({ type: "selfupdate", frac: f }); if (f >= 1) clearInterval(t); }, 120); }],
    ]],
    ["Asistente", [
      ["1 · Tu PC", wizard("premium", "prism", 0)],
      ["2 · Ajustes (premium)", wizard("premium", "prism", 1)],
      ["2 · Ajustes (no premium)", wizard("nopremium", "freesm", 1)],
      ["3 · Resumen", wizard("nopremium", "freesm", 2)],
      ["Aviso de RAM (24 GB)", () => { wizard("nopremium", "freesm", 1)(); setTimeout(() => askRam(24), 500); }],
    ]],
    ["Instalación", [
      ["Instalando (simulado)", () => { D.fail = false; newW("nopremium", "freesm"); S.w.nick = "Endo"; startInstall(false, false); }],
      ["Instalando → error", () => { D.fail = true; newW("nopremium", "freesm"); S.w.nick = "Endo"; startInstall(false, false); }],
      ["Pantalla de error", () => { S.lastError = "Error de prueba"; show("error", { message: "No se pudo descargar el archivo (HTTP 503) [simulado]", tail: "[04:20:11] Instalar: freesm Medio java21\n[04:20:14] ERROR: HTTP 503" }); }],
    ]],
    ["Pantalla principal", [
      ["Al día", home("none")],
      ["Hay actualización (naranja)", home("new")],
      ["Sin conexión", home("offline")],
      ["Borrar instancia", () => { home("none")(); setTimeout(() => ACT.del(), 700); }],
    ]],
    ["Guías", [
      ["Guía de TLauncher", () => { S.w = { kind: "otro", mode: "tlauncher", profile: "Medio", variant: "java21", ram: 6, base }; ACT.play(); }],
      ["Iniciar sesión (premium)", () => { S.w = { kind: "premium", mode: "prism", profile: "Medio", variant: "java21", ram: 6, base }; openLoginGuide(); }],
    ]],
  ];
  window.DEMO_RUN = (key) => { const all = {}; GROUPS.forEach(([g, it]) => it.forEach(([n, f]) => (all[n] = f))); (all[key] || (() => {}))(); };
  const panel = document.createElement("div");
  panel.id = "demo"; panel.classList.add("shut");
  panel.innerHTML = `<div class="dhead" id="dhead">DEMO · estados <span id="dtog">▾</span></div><div class="dbody" id="dbody">${GROUPS.map(([g, items]) => `<div class="dg">${g}</div>${items.map(([n], i) => `<button data-g="${g}" data-i="${i}">${n}</button>`).join("")}`).join("")}</div>`;
  document.body.appendChild(panel);
  panel.addEventListener("click", (e) => {
    const b = e.target.closest("button");
    if (b) { const g = GROUPS.find((x) => x[0] === b.dataset.g); g[1][+b.dataset.i][1](); return; }
    if (e.target.closest("#dhead")) panel.classList.toggle("shut");
  });
  const css = document.createElement("style");
  css.textContent = `#demo{position:fixed;right:14px;top:64px;z-index:200;width:230px;background:rgba(14,12,22,.94);border:1px solid rgba(255,255,255,.18);border-radius:14px;font:12px "Segoe UI",sans-serif;color:#eee;box-shadow:0 12px 40px rgba(0,0,0,.6)}
  #demo .dhead{padding:9px 12px;font-weight:700;letter-spacing:.5px;cursor:pointer;display:flex;justify-content:space-between;color:#9be8a8}
  #demo .dbody{max-height:calc(100vh - 150px);overflow:auto;padding:0 8px 8px}#demo.shut .dbody{display:none}
  #demo .dg{margin:8px 4px 3px;color:#a9a6b8;font-size:10px;text-transform:uppercase;letter-spacing:.8px}
  #demo button{display:block;width:100%;text-align:left;margin:2px 0;padding:6px 9px;border:0;border-radius:7px;background:rgba(255,255,255,.07);color:#eee;font:12px "Segoe UI",sans-serif;cursor:pointer}
  #demo button:hover{background:rgba(155,232,168,.25)}`;
  document.head.appendChild(css);
})();
