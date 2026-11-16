/* GitanoLand Launcher - interfaz. HTML/CSS/JS normal: edita a gusto. El "backend" (instalar, jugar...) es Python (pywebview.api). */
"use strict";
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const S = { cfg: {}, w: {}, sys: { ram_total: 16, version: "?" }, screen: null, update: null, installed: null };
const REAL = () => window.pywebview && window.pywebview.api;
const API = () => (window.__MOCK__ ? window.MOCK_API : REAL());   // en el modo demo/mock todo es simulado salvo la ventana
const RECOMMENDED = { Alto: 8, Medio: 6, Bajo: 4 };
const PROFILE_TEXT = {
  Alto: "PC bueno. Shaders y todos los efectos. Recomendado: 8 GB de RAM o más.",
  Medio: "PC normal. Shaders ligeros, sin extras pesados. Recomendado: 6 GB de RAM.",
  Bajo: "PC malillo. Sin shaders ni extras pesados. Recomendado: 4 GB de RAM.",
};
const OTHERS = {
  tlauncher: ["TLauncher", "Lo preparo todo y te guío paso a paso por TLauncher."],
  curseforge: ["CurseForge", "Te guío para crear el perfil y lo relleno yo."],
  otro: ["Otro launcher", "Para cualquier otro: te digo qué poner y dónde."],
};
const esc = (s) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
const isFull = (m) => m === "prism" || m === "freesm";
const maxSafeRam = () => Math.max(2, Math.floor(S.sys.ram_total - 2));

/* ---------------------------------------------------------------- fondo promocional */
const PROMOS = Array.from({ length: 12 }, (_, i) => `img/promo${i + 1}.webp`).sort(() => Math.random() - 0.5);
let promoIdx = 0;
function startSlides() {
  const bg = $("#bg");
  PROMOS.slice(0, 6).forEach((p) => { const d = document.createElement("div"); d.className = "slide"; d.style.backgroundImage = `url(${p})`; bg.prepend(d); });
  const slides = $$("#bg .slide");
  const next = () => { slides.forEach((s) => s.classList.remove("on")); const s = slides[promoIdx++ % slides.length]; void s.offsetWidth; s.classList.add("on"); };
  next();
  setInterval(next, 9000);
}
function bgMode(mode) { document.body.classList.toggle("hero", mode === "hero"); }   // solo la pantalla principal enseña la foto de fondo

/* ---------------------------------------------------------------- utilidades de interfaz */
function toast(text, err = false, ms = 2800) {
  const t = $("#toast"); t.textContent = text; t.className = "show" + (err ? " err" : "");
  clearTimeout(toast.h); toast.h = setTimeout(() => (t.className = ""), ms);
}
function modal(html, onMount) {
  const m = $("#modal"); m.innerHTML = `<div class="dialog">${html}</div>`; m.classList.add("open");
  onMount && onMount(m);
}
function closeModal() { $("#modal").classList.remove("open"); }

let currentScreen = null;
function show(name, args = {}, dir = 1) {
  const view = $("#view");
  const prev = currentScreen;
  const el = document.createElement("section");
  currentScreen = el;                              // siempre manda la ULTIMA pantalla pedida (aunque cambies muy rapido)
  const home = SCREENS[name].cls === "home";
  el.className = "screen" + (SCREENS[name].cls ? " " + SCREENS[name].cls : "");
  el.innerHTML = home ? SCREENS[name].render(args) : `<div class="wrap">${SCREENS[name].render(args)}</div>`;
  view.appendChild(el);
  bgMode(SCREENS[name].bg);
  S.screen = name;
  // al instante: fuera todo lo que no sea la pantalla que se va ni la nueva (nunca se acumulan)
  $$(".screen", view).forEach((n) => { if (n !== el && n !== prev) n.remove(); });
  if (prev) { prev.classList.remove("in"); prev.classList.add("out"); setTimeout(() => { if (prev !== currentScreen) prev.remove(); }, 300); }
  let started = false;
  const start = () => {                            // rAF y un respaldo con timeout: si uno no salta, el otro enseña la pantalla
    if (started) return;
    started = true;
    if (el !== currentScreen) return;
    el.classList.add("in");
    SCREENS[name].mount && SCREENS[name].mount(el, args);
  };
  requestAnimationFrame(start);
  setTimeout(start, 80);
}

/* ---------------------------------------------------------------- botones: se quedan pulsados mientras se mantiene el clic; soltar fuera no hace nada */
const ACT = {};
document.addEventListener("pointerdown", (e) => {
  if (e.button !== 0) return;
  const el = e.target.closest("[data-act]");
  if (!el || el.hasAttribute("disabled")) { if (!e.target.closest(".menu")) $$(".menu.open").forEach((m) => m.classList.remove("open")); return; }
  el.classList.add("pressed");
  try { el.setPointerCapture(e.pointerId); } catch (_) { /* nada */ }
  const up = (ev) => {
    el.classList.remove("pressed");
    el.removeEventListener("pointerup", up); el.removeEventListener("pointercancel", cancel);
    const r = el.getBoundingClientRect();
    const inside = ev.clientX >= r.left && ev.clientX <= r.right && ev.clientY >= r.top && ev.clientY <= r.bottom;
    if (inside) { $$(".menu.open").forEach((m) => m.classList.remove("open")); const f = ACT[el.dataset.act]; f && f(el.dataset.arg, el); }
  };
  const cancel = () => { el.classList.remove("pressed"); el.removeEventListener("pointerup", up); el.removeEventListener("pointercancel", cancel); };
  el.addEventListener("pointerup", up); el.addEventListener("pointercancel", cancel);
});

/* ---------------------------------------------------------------- pantallas */
const SCREENS = {};

SCREENS.welcome = {
  bg: "",
  render: () => `
    <h1>¿Cómo <b>juegas</b>?</h1>
    <p class="sub">Elige una opción. Yo te llevo paso a paso.</p>
    <div class="cards">
      <div class="card prem" data-act="choose" data-arg="premium">
        <div class="pic" style="background-image:url(img/premium.jpg)"></div>
        <div class="body"><h2>Premium</h2><p>Tengo cuenta de Minecraft (Microsoft). Entro con mi cuenta oficial.</p><div class="go">Elegir <i>→</i></div></div>
      </div>
      <div class="card nop" data-act="choose" data-arg="nopremium">
        <div class="pic" style="background-image:url(img/nopremium.jpg)"></div>
        <div class="body"><h2>No premium</h2><p>Aún no me he comprado Minecraft. Entro con un nombre de jugador (servidores sin verificación).</p><div class="go">Elegir <i>→</i></div></div>
      </div>
    </div>
    <div class="row center" style="margin-top:26px"><a class="link" data-act="other">Usa otro launcher (TLauncher, CurseForge…) <span>→</span></a></div>`,
};

SCREENS.other = {
  bg: "",
  render: () => `
    <h1>Otro <b>launcher</b></h1><p class="sub">Elige el que usas. Te digo exactamente qué hacer en cada paso.</p>
    <div class="cards three">
      ${Object.entries(OTHERS).map(([k, [n, b]]) => `<div class="card mini" data-act="chooseOther" data-arg="${k}"><div class="body"><h2>${n}</h2><p>${b}</p><div class="go">Elegir <i>→</i></div></div></div>`).join("")}
    </div>
    <div class="footer"><div class="btn" data-act="toWelcome">←  Atrás</div></div>`,
};

function stepper(step) {
  const names = ["Tu PC", "Ajustes", "Instalar"];
  return `<div class="stepper">${names.map((n, i) => `${i ? '<span class="line"></span>' : ""}<span class="item ${i < step ? "done" : i === step ? "on" : ""}"><span class="dot">${i < step ? "✓" : i + 1}</span>${n}</span>`).join("")}${S.w.edit ? '<span class="spacer"></span><b style="color:var(--orange-hi);letter-spacing:1px;font-size:11px">EDITAR INSTANCIA</b>' : ""}</div>`;
}
const footer = (step, next) => `<div class="footer"><div class="btn" data-act="wBack" data-arg="${step}">←  Atrás</div><span class="spacer"></span><div class="btn primary" data-act="wNext" data-arg="${step}">${next}</div></div>`;

SCREENS.wizard0 = {
  bg: "",
  render: () => `${stepper(0)}<h1>¿Cómo va <b>tu PC</b>?</h1><p class="sub">Elige el que mejor se parezca. Puedes cambiarlo luego.</p>
    <div class="cards three">
      ${["Alto", "Medio", "Bajo"].map((p) => `<div class="card mini ${S.w.profile === p ? "sel" : ""}" data-act="pickProfile" data-arg="${p}"><div class="body"><h2>${p}</h2><p>${PROFILE_TEXT[p]}</p></div></div>`).join("")}
    </div>${footer(0, "Siguiente  →")}`,
};

SCREENS.wizard1 = {
  bg: "",
  render: () => {
    const w = S.w, chips = [4, 6, 8, 12];
    return `${stepper(1)}<h1><b>Ajustes</b></h1><p class="sub">Memoria, versión de Java y dónde se guarda.</p>
    <div class="panel"><div class="sec"><div class="t">Memoria (RAM) <span style="color:var(--muted);font-weight:400">— tu PC tiene ${S.sys.ram_total} GB</span></div>
      <div class="chips" id="ramchips">
        ${chips.map((v) => `<div class="chip ${w.ram === v ? "on" : ""} ${v > maxSafeRam() ? "warnchip" : ""}" data-act="ram" data-arg="${v}">${v} GB</div>`).join("")}
        <div class="chip ${chips.includes(w.ram) ? "" : "on"}" data-act="ramCustom">Otro</div>
        <input class="txt" id="ramcustom" style="width:84px;display:${chips.includes(w.ram) ? "none" : "block"}" value="${chips.includes(w.ram) ? "" : w.ram}" placeholder="GB">
      </div>
      <div class="hint ${w.ram > maxSafeRam() ? "warn" : ""}" id="ramhint">${w.ram > maxSafeRam() ? "⚠ Es más de lo recomendado para tu PC." : `Recomendado para tu PC: hasta ${maxSafeRam()} GB.`}</div></div>
    <div class="sec"><div class="t">Versión de Java</div><div class="chips">
      <div class="chip ${w.variant === "java21" ? "on" : ""}" data-act="java" data-arg="java21">Java 21 (recomendada)</div>
      <div class="chip ${w.variant === "java25" ? "on" : ""}" data-act="java" data-arg="java25">Java 25 (con intro nueva)</div></div></div>
    ${w.kind === "nopremium" ? `<div class="sec"><div class="t">Tu nombre de jugador</div><input class="txt" id="nick" maxlength="16" value="${esc(w.nick || "")}" placeholder="Por ejemplo: Endo"></div>` : ""}
    <div class="sec"><div class="t">Carpeta de instalación</div><div class="row"><div class="path" id="basepath">${esc(w.base)}</div><div class="btn small" data-act="pickFolder">Cambiar carpeta…</div></div></div></div>
    ${footer(1, "Siguiente  →")}`;
  },
};

SCREENS.wizard2 = {
  bg: "",
  render: () => {
    const w = S.w;
    const how = { premium: "Premium (cuenta de Microsoft)", nopremium: "No premium (nombre de jugador)" }[w.kind] || (OTHERS[w.mode] || ["Otro launcher"])[0];
    const rows = [["Cómo juegas", how]];
    if (w.kind === "nopremium") rows.push(["Jugador", w.nick]);
    rows.push(["Perfil", w.profile], ["Memoria", `${w.ram} GB`], ["Java", w.variant === "java21" ? "Java 21" : "Java 25"], ["Carpeta", w.base]);
    return `${stepper(2)}<h1><b>${w.edit ? "Guardar cambios" : "Todo listo"}</b></h1><p class="sub">Revisa que esté bien y pulsa el botón verde.</p>
      <div class="panel">${rows.map(([k, v]) => `<div class="sumrow"><b>${k}</b><span>${esc(v)}</span></div>`).join("")}</div>${footer(2, w.edit ? "Guardar" : "Instalar")}`;
  },
};

SCREENS.install = {
  bg: "",
  render: (a) => `<div class="center"><h1><b>${a.update ? "Actualizando" : "Instalando"}</b> GitanoLand</h1>
    <p class="sub">No cierres esta ventana. Puede tardar unos minutos la primera vez.</p>
    <div class="steps" id="isteps">${["Preparando el launcher", "Descargando los archivos", "Ajustando tu configuración", "Terminando"].map((t) => `<div class="s"><span class="ic"></span>${t}</div>`).join("")}</div>
    <div class="bar busy" id="ibar"><i></i></div><div class="status" id="istatus"></div></div>`,
};

SCREENS.home = {
  bg: "hero",
  cls: "home",
  render: () => {
    const w = S.w, name = { prism: "Prism", freesm: "Freesm" }[w.mode] || (OTHERS[w.mode] || ["Otro"])[0];
    const who = w.kind === "nopremium" ? `Jugador: ${esc(w.nick)}` : w.kind === "premium" ? "Cuenta de Microsoft" : name;
    return `<div class="hero"><img src="img/logo.png" alt="GitanoLand"><div class="tagline">${who} · <span id="tag">${esc(S.cfg.tag || "")}</span></div></div>
    <div class="bottombar">
      <div class="left"><div class="pills"><span class="pill"><b>Perfil</b>${w.profile}</span><span class="pill"><b>Java</b>${w.variant === "java21" ? "21" : "25"}</span><span class="pill"><b>RAM</b>${w.ram} GB</span><span class="pill"><b>Launcher</b>${name}</span></div>
        <div class="row">
          <div class="btn small" data-act="edit">Editar instancia</div><div class="btn small" data-act="reinstall">Reinstalar</div><div class="btn small danger" data-act="del">Borrar</div>
        </div>
        <div class="upmsg" id="upmsg">Buscando actualizaciones…</div></div>
      <div class="split" id="split"><div class="btn primary big" id="playbtn" data-act="play">${isFull(w.mode) ? "Jugar" : "Abrir la guía"}</div>
        <div class="btn primary big dots" id="dotsbtn" data-act="menu" style="display:none">⋮</div>
        <div class="menu" id="playmenu"><div class="item" data-act="playNoUpdate">Jugar sin actualizar</div></div></div>
    </div>`;
  },
  mount: () => checkUpdate(),
};

SCREENS.guide = {
  bg: "",
  render: (a) => { S.guide = { ...a, i: 0 }; return `<div id="gwrap"></div><div class="footer"><div class="btn" data-act="gPrev">←  Atrás</div><span class="spacer"></span><div class="btn primary" data-act="gNext">Hecho  ✓</div></div>`; },
  mount: () => drawGuide(),
};

SCREENS.error = {
  bg: "plain",
  render: (a) => `<div class="center" style="padding-top:8px"><h1 style="color:var(--red)">Algo ha fallado</h1>
    <p class="sub" style="max-width:700px">${esc(a.message).slice(0, 500)}</p>
    <p class="sub" style="max-width:700px;margin-top:14px;color:#d6d8df">Guardé un archivo <b>.log</b> con lo que pasó. <b>Mándamelo por Discord</b> o crea el aviso en GitHub (te lo dejo ya preparado, solo le das a enviar).</p>
    <pre class="log" style="width:700px;text-align:left" id="errlog">${esc(a.tail || "")}</pre>
    <div class="row" style="margin-top:22px;flex-wrap:wrap;justify-content:center">
      <div class="btn primary" data-act="retry">Reintentar</div><div class="btn" data-act="reportIssue">Crear aviso en GitHub</div>
      <div class="btn" data-act="copyReport">Copiar informe</div><div class="btn" data-act="openLogs">Abrir carpeta del .log</div></div></div>`,
};

SCREENS.system = {
  bg: "soft",
  render: () => `<h1>Instalar GitanoLand en tu PC</h1><p class="sub">Así lo abres siempre desde el escritorio o el menú inicio.</p>
    <div class="gcard"><div class="k">RECOMENDADO</div><h2>Instalación en tu usuario</h2><p>Copio el launcher a tu usuario (no hace falta ser administrador) y creo un acceso directo en el escritorio y en el menú inicio. Se actualiza solo.</p></div>
    <div class="row" style="margin-top:22px"><div class="btn primary" data-act="sysInstall">Instalar (recomendado)</div><div class="btn" data-act="sysSkip">Ahora no</div></div><div class="hint" id="sysmsg"></div>`,
};

SCREENS.selfupdate = {
  bg: "plain",
  render: () => `<div class="center" style="padding-top:90px"><h1>Actualizando el launcher…</h1><p class="sub">Solo tarda un momento. Se reiniciará solo.</p>
    <div class="bar" style="margin-top:26px"><i id="subar"></i></div></div>`,
};

/* ---------------------------------------------------------------- acciones */
function newW(kind, mode) {
  S.w = { kind, mode, profile: S.cfg.profile || "Medio", variant: S.cfg.variant || "java21", ram: null, nick: S.cfg.nick || "", base: S.sys.base, ramTouched: false, edit: false, reinstall: !!S.w.reinstall };
  S.w.ram = RECOMMENDED[S.w.profile];
}
ACT.choose = (kind) => { newW(kind, kind === "premium" ? "prism" : "freesm"); show("wizard0"); };
ACT.other = () => show("other");
ACT.chooseOther = (mode) => { newW("otro", mode); show("wizard0"); };
ACT.toWelcome = () => show("welcome", {}, -1);
ACT.pickProfile = (p, el) => { S.w.profile = p; if (!S.w.ramTouched) S.w.ram = RECOMMENDED[p]; $$(".card.mini").forEach((c) => c.classList.toggle("sel", c.dataset.arg === p)); };
ACT.wBack = (step) => { step = +step; if (step === 0) show(S.w.edit ? "home" : "welcome", {}, -1); else show("wizard" + (step - 1), {}, -1); };
ACT.wNext = async (step) => {
  step = +step;
  if (step === 1) {
    if (S.w.kind === "nopremium") {
      const nick = ($("#nick").value || "").replace(/[^A-Za-z0-9_]/g, "").slice(0, 16);
      if (nick.length < 3) return toast("Escribe tu nombre de jugador (mínimo 3 letras o números).", true);
      S.w.nick = nick;
    }
    const cu = $("#ramcustom");
    if (cu && cu.style.display !== "none") {
      const v = parseFloat(String(cu.value).replace(",", "."));
      if (!(v >= 2 && v <= 64)) return toast("La RAM tiene que ser un número entre 2 y 64.", true);
      S.w.ram = v;
    }
    if (S.w.ram > maxSafeRam() && !S.w.ramRiskOk) return askRam(S.w.ram, () => show("wizard2"));
  }
  if (step < 2) show("wizard" + (step + 1)); else askExisting(() => startInstall(false, false));
};
ACT.ram = (v) => { v = +v; if (v > maxSafeRam() && !S.w.ramRiskOk) return askRam(v); setRam(v); };
ACT.ramCustom = () => { $("#ramcustom").style.display = "block"; $("#ramcustom").focus(); $$("#ramchips .chip").forEach((c) => c.classList.remove("on")); $$("#ramchips .chip")[4].classList.add("on"); S.w.ramTouched = true; };
function setRam(v) {
  S.w.ram = v; S.w.ramTouched = true;
  $$("#ramchips .chip").forEach((c) => c.classList.toggle("on", c.dataset.arg == v));
  $("#ramcustom").style.display = "none";
  const h = $("#ramhint"); h.className = "hint" + (v > maxSafeRam() ? " warn" : ""); h.textContent = v > maxSafeRam() ? "⚠ Es más de lo recomendado para tu PC." : `Recomendado para tu PC: hasta ${maxSafeRam()} GB.`;
}
function askRam(v, after) {
  modal(`<h3>Eso es más RAM de la que tu PC puede dar</h3>
    <p>Tu PC tiene <b>${S.sys.ram_total} GB</b> y has puesto <b>${v} GB</b>. Si le das tanta al juego, el sistema se queda sin memoria y todo puede ir <b>peor</b>, colgarse o no abrir.</p>
    <p>Lo recomendable es como máximo <b>${maxSafeRam()} GB</b>.</p>
    <div class="row"><div class="btn" id="m-risk">Insistir</div><div class="btn primary" id="m-safe">Poner ${maxSafeRam()} GB (recomendado)</div></div>`, (m) => {
    $("#m-safe").onclick = () => { closeModal(); if (S.screen === "wizard1") setRam(maxSafeRam()); else S.w.ram = maxSafeRam(); after && after(); };
    $("#m-risk").onclick = () => {
      modal(`<h3>Bajo tu propia responsabilidad</h3>
        <p>Vale. Pero <b>a partir de aquí, lo que pase</b> (juego lento, cierres, el PC colgado o que no abra) <b>es bajo tu propia responsabilidad</b>. No voy a poder ayudarte con eso.</p>
        <label class="chk"><input type="checkbox" id="m-ok"> Lo entiendo y lo hago bajo mi responsabilidad</label>
        <div class="row"><div class="btn" id="m-cancel">Cancelar</div><div class="btn primary" id="m-accept" disabled>Aceptar</div></div>`, () => {
        $("#m-ok").onchange = (e) => (e.target.checked ? $("#m-accept").removeAttribute("disabled") : $("#m-accept").setAttribute("disabled", ""));
        $("#m-cancel").onclick = closeModal;
        $("#m-accept").onclick = () => { S.w.ramRiskOk = true; closeModal(); if (S.screen === "wizard1") setRam(v); else S.w.ram = v; after && after(); };
      });
    };
  });
}
ACT.java = (v) => { S.w.variant = v; $$("[data-act=java]").forEach((c) => c.classList.toggle("on", c.dataset.arg === v)); };
ACT.pickFolder = async () => { const d = await API().pick_folder(S.w.base); if (d) { S.w.base = d; $("#basepath").textContent = d; } };

/* ¿ya tienes Prism/Freesm? */
function askExisting(go) {
  S.w.useExisting = false;
  Promise.resolve(API().detect_launcher(S.w.mode)).then((f) => {
    if (!f) return go();
    modal(`<h3>Ya tienes ${f.name} instalado</h3>
      <p>He encontrado tu <b>${f.name}</b> en <b>${f.exe}</b>.</p>
      <p>Puedo meter la instancia de GitanoLand ahí, sin instalar otro (no toco tus otras instancias ni tus cuentas). O instalar uno aparte solo para GitanoLand.</p>
      <div class="row"><div class="btn" id="m-new">Instalar uno aparte</div><div class="btn primary" id="m-use">Usar el que tengo</div></div>`, () => {
      $("#m-use").onclick = () => { S.w.useExisting = true; closeModal(); go(); };
      $("#m-new").onclick = () => { S.w.useExisting = false; closeModal(); go(); };
    });
  }).catch(() => go());
}

/* instalacion */
function startInstall(update, playAfter) {
  S.installCtx = { update, playAfter };
  show("install", { update });
  setTimeout(() => API().start_install(S.w, update), 450);
}
window.GL = {
  event(ev) {
    if (ev.type === "step") {
      $$("#isteps .s").forEach((s, i) => { s.classList.toggle("done", i < ev.i); s.classList.toggle("on", i === ev.i); });
    } else if (ev.type === "progress") {
      const b = $("#ibar"); if (!b) return; b.classList.remove("busy"); $("i", b).style.width = Math.round(ev.frac * 100) + "%";
    } else if (ev.type === "busy") { const b = $("#ibar"); b && b.classList.add("busy"); }
    else if (ev.type === "log") { const s = $("#istatus"); if (s) s.textContent = ev.text; }
    else if (ev.type === "done") onInstallDone(ev);
    else if (ev.type === "fail") { S.lastError = ev.message; show("error", ev); }
    else if (ev.type === "selfupdate") { const b = $("#subar"); if (b) b.style.width = Math.round(ev.frac * 100) + "%"; }
    else if (ev.type === "selfupdate_start") show("selfupdate");
    else if (ev.type === "toast") toast(ev.text, ev.err);
  },
};
function onInstallDone(ev) {
  S.cfg = ev.cfg; S.w.base = ev.cfg.base || S.w.base; S.w.reinstall = false; S.w.edit = false;
  $$("#isteps .s").forEach((s) => { s.classList.remove("on"); s.classList.add("done"); });
  setTimeout(() => {
    if (!isFull(S.w.mode)) return openGuide();
    show("home");
    if (S.installCtx && S.installCtx.playAfter) setTimeout(launch, 900); else setTimeout(() => toast("¡Instalado! Pulsa Jugar."), 700);
  }, 700);
}

/* pantalla principal */
async function checkUpdate() {
  const r = await API().pending_update(S.w);
  const msg = $("#upmsg"); if (!msg) return;
  S.update = null;
  if (S.cfg && r.installed) { const t = $("#tag"); if (t) t.textContent = r.installed; }
  if (!r.latest) { msg.textContent = "Sin conexión: puedes jugar con la versión que ya tienes."; return; }
  if (r.installed !== r.latest) {
    S.update = r;
    msg.className = "upmsg new"; msg.textContent = `Hay una actualización: ${r.installed || "?"}  →  ${r.latest}`;
    if (isFull(S.w.mode)) {
      const p = $("#playbtn"); p.textContent = "Actualizar"; p.classList.remove("primary"); p.classList.add("orange");
      const d = $("#dotsbtn"); d.style.display = "inline-flex"; d.classList.remove("primary"); d.classList.add("orange"); d.style.animation = "none";
    }
  } else { msg.className = "upmsg"; msg.textContent = "Tienes la última versión."; }
}
ACT.menu = () => $("#playmenu").classList.toggle("open");
ACT.play = () => { if (!isFull(S.w.mode)) return openGuide(); if (S.update) startInstall(true, true); else launch(); };
ACT.playNoUpdate = () => launch();
async function launch() {
  const r = await API().launch(S.w);
  if (r.need_login) return openLoginGuide();
  if (r.error) return show("error", { message: r.error, tail: r.tail || "" });
  toast("Abriendo el juego…");
  setTimeout(() => API().close(), 2600);
}
ACT.edit = () => { S.w = { ...S.w, edit: true, reinstall: false, ramTouched: true, ramRiskOk: S.w.ram > maxSafeRam() }; show("wizard0"); };
ACT.reinstall = () => { S.w = { ...S.w, edit: false, reinstall: true }; show("welcome"); };
ACT.del = () => modal(`<h3>Borrar la instancia</h3>
  <p>Se borra la instancia <b>${esc(S.w.profile)}</b> con sus mods, ajustes y <b>mundos guardados</b>. <b>No se puede deshacer.</b></p>
  <label class="chk"><input type="checkbox" id="d-l"> Borrar también el launcher descargado (${S.w.mode === "freesm" ? "Freesm" : "Prism"})</label>
  <label class="chk"><input type="checkbox" id="d-ok"> Entiendo que se pierden mis mundos y ajustes</label>
  <div class="row"><div class="btn" id="d-no">Cancelar</div><div class="btn danger" id="d-yes" disabled>Borrar</div></div>`, () => {
  $("#d-ok").onchange = (e) => (e.target.checked ? $("#d-yes").removeAttribute("disabled") : $("#d-yes").setAttribute("disabled", ""));
  $("#d-no").onclick = closeModal;
  $("#d-yes").onclick = async () => { const also = $("#d-l").checked; closeModal(); const r = await API().delete_instance(S.w, also); if (r.ok) { S.cfg = r.cfg; toast("Instancia borrada."); S.w = {}; show("welcome", {}, -1); } else toast(r.error || "No se pudo borrar.", true); };
});

/* guias */
async function openGuide() { const g = await API().guide(S.w); show("guide", g); }
async function openLoginGuide() { const g = await API().login_guide(S.w); show("guide", { ...g, finish: "home" }); }
function drawGuide() {
  const g = S.guide, s = g.steps[g.i];
  $("#gwrap").innerHTML = `<h1>${esc(g.title)}</h1><p class="sub">Un paso cada vez. Cuando lo hagas, pulsa «Hecho».</p>
    <div class="gcard"><div class="k">PASO ${g.i + 1} DE ${g.steps.length}</div><h2>${esc(s.t)}</h2><p>${esc(s.b)}</p>
    ${s.copy ? `<div class="copybox"><input class="txt" readonly value="${esc(s.copy)}"><div class="btn small" data-act="copyText" data-arg="${esc(s.copy)}">Copiar</div><div class="btn small" data-act="openPath" data-arg="${esc(s.open || "")}" style="display:${s.open ? "inline-flex" : "none"}">Abrir carpeta</div></div>` : ""}</div>
    <div class="gdots">${g.steps.map((_, i) => `<i class="${i <= g.i ? "on" : ""}"></i>`).join("")}</div>`;
}
ACT.gNext = () => { const g = S.guide; if (g.i < g.steps.length - 1) { g.i++; drawGuide(); } else show("home", {}, -1); };
ACT.gPrev = () => { const g = S.guide; if (g.i > 0) { g.i--; drawGuide(); } };
ACT.copyText = (t) => { navigator.clipboard.writeText(t).then(() => toast("Copiado")); };
ACT.openPath = (p) => API().open_path(p);

/* errores */
ACT.retry = () => startInstall(false, false);
function copyText(t) {
  try { return navigator.clipboard.writeText(t).catch(() => legacyCopy(t)); } catch (e) { return legacyCopy(t); }
}
function legacyCopy(t) {
  const a = document.createElement("textarea"); a.value = t; a.style.position = "fixed"; a.style.opacity = "0"; document.body.appendChild(a); a.select();
  try { document.execCommand("copy"); } catch (e) { /* nada */ } a.remove();
}
ACT.reportIssue = async () => {
  try {
    const r = await API().make_report(S.lastError || "");
    await copyText(r.text);
    await API().open_url(r.url);
    toast("Abro GitHub con un resumen. El informe completo está copiado: pégalo (Ctrl+V) en el aviso y envíalo.", false, 6000);
  } catch (e) { toast("No se pudo abrir GitHub. Usa «Copiar informe» y pégalo en Discord.", true, 6000); }
};
ACT.copyReport = async () => { const r = await API().make_report(S.lastError || ""); navigator.clipboard.writeText(r.text).then(() => toast("Informe copiado. Pégalo en Discord.")); };
ACT.openLogs = () => API().open_logs();

/* instalar en el sistema */
ACT.sysInstall = async () => { $("#sysmsg").textContent = "Instalando…"; const r = await API().install_system(); if (!r.ok) { $("#sysmsg").textContent = "No se pudo instalar: " + r.error; $("#sysmsg").className = "hint warn"; } };
ACT.sysSkip = async () => { await API().skip_system(); boot(true); };

/* ---------------------------------------------------------------- arranque */
$("#min").onclick = () => REAL() && REAL().minimize();
$("#close").onclick = () => REAL() && REAL().close();
async function boot(skipSys) {
  const init = await API().init();
  try { API().client_info && API().client_info({ iw: innerWidth, ih: innerHeight, dpr: devicePixelRatio, sw: screen.width, sh: screen.height, ow: outerWidth, oh: outerHeight }); } catch (_) { /* nada */ }
  S.sys = init.sys; S.cfg = init.cfg; $("#ver").textContent = "launcher v" + init.sys.version;
  if (init.sys.frozen && !init.sys.installed_here && !init.cfg.skip_install && !skipSys) return show("system");
  if (init.cfg.installed) { S.w = init.w; return show("home"); }
  show("welcome");
}
if (location.search.includes('lowfx')) document.body.classList.add('lowfx');
function loadScript(src) { return new Promise((ok, fail) => { const s = document.createElement("script"); s.src = src; s.onload = ok; s.onerror = fail; document.head.appendChild(s); }); }
window.addEventListener("pywebviewready", async () => {
  startSlides();
  try {   // exe de DEMO: backend simulado + panel de estados (nada se descarga ni instala)
    if (REAL().is_demo && (await REAL().is_demo())) { await loadScript("mock.js"); await loadScript("demo.js"); window.__MOCK__ = true; }
  } catch (_) { /* nada */ }
  await boot();
  try { const st = window.__MOCK__ && REAL().demo_state && (await REAL().demo_state()); if (st) setTimeout(() => window.DEMO_RUN(st), 900); } catch (_) { /* nada */ }
});
if (window.__MOCK__) { startSlides(); boot(); }
