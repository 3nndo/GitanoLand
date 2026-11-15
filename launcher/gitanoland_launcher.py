"""GitanoLand Launcher 2.x: instala y actualiza GitanoLand paso a paso (Premium / No premium) y lo abre.

La interfaz es HTML/CSS/JS (carpeta ui/) dentro de una ventana sin bordes (pywebview + WebView2).
Este archivo es el "backend": instalar, actualizar, jugar, RAM, borrar, errores... usando gl_core.py.
- Premium = Prism Launcher portable propio (cuenta de Microsoft). No premium = Freesm Launcher portable propio (solo un nombre).
- Otros launchers (TLauncher, CurseForge...): guia paso a paso.
- Nunca pisa options.txt (sensibilidad, teclas) ni servers.dat al actualizar. Se actualiza solo.
"""
import gl_selfcheck

gl_selfcheck.ensure_own_bundle()  # antes de importar nada mas (ver gl_selfcheck)

import json
import os
import shutil
import subprocess
import sys
import threading
import time
import traceback
import webbrowser

if sys.platform.startswith("linux"):
    # Linux: la ventana usa Qt WebEngine (viene dentro del AppImage). El sandbox de Chromium no funciona en muchas distros
    # sin permisos especiales; aqui solo se carga la interfaz local del launcher.
    os.environ.setdefault("PYWEBVIEW_GUI", "qt")
    os.environ.setdefault("QTWEBENGINE_CHROMIUM_FLAGS", "--no-sandbox")

import webview

import gl_core as core

PROFILE_RAM = {"Alto": 8, "Medio": 6, "Bajo": 4}
OTHER_NAMES = {"tlauncher": "TLauncher", "curseforge": "CurseForge", "otro": "tu launcher"}


def state_key(mode, profile, variant):
    base = profile if mode == "prism" else f"{mode}:{profile}"
    return base + ("@java25" if variant == "java25" else "")


class Api:
    """Todo lo que la interfaz HTML puede pedirle a Python (window.pywebview.api.<metodo>)."""

    def __init__(self, demo=False):
        self._demo = demo
        self._window = None
        self._cfg = core.load_state().get("_config", {})
        if self._cfg.get("base"):
            core.BASE[0] = self._cfg["base"]
        self._last_error = ""

    # ---------------------------------------------------------- utilidades
    def emit(self, **ev):
        try:
            self._window.evaluate_js("GL.event(" + json.dumps(ev) + ")")
        except Exception:  # noqa: BLE001
            pass

    def save_cfg(self, **kw):
        self._cfg.update(kw)
        st = core.load_state()
        st["_config"] = self._cfg
        core.save_state(st)

    def wdict(self):
        c, prof = self._cfg, self._cfg.get("profile", "Medio")
        return {"kind": c.get("kind", "premium"), "mode": c.get("mode", "prism"), "profile": prof,
                "variant": c.get("variant", "java21"), "ram": c.get("ram", PROFILE_RAM.get(prof, 6)), "nick": c.get("nick", ""),
                "base": core.base_dir(), "ramTouched": True}

    # ---------------------------------------------------------- arranque
    def init(self):
        core.cleanup_old_exe()
        core.log("Arranque del launcher " + core.LAUNCHER_VERSION)
        threading.Thread(target=self._check_launcher_update, daemon=True).start()
        cfg = dict(self._cfg)
        key = state_key(cfg.get("mode", "prism"), cfg.get("profile", "Medio"), cfg.get("variant", "java21"))
        cfg["tag"] = core.load_state().get(key, {}).get("tag", "")
        return {"sys": {"ram_total": core.ram_total_gb() or 16, "version": core.LAUNCHER_VERSION, "base": core.base_dir(),
                        "frozen": bool(getattr(sys, "frozen", False)), "installed_here": core.installed_here()},
                "cfg": cfg, "w": self.wdict()}

    def _check_launcher_update(self):
        """El launcher se actualiza solo (sin preguntar): baja la version nueva y se reinicia."""
        me = core.self_path()
        if not me:
            return
        try:
            newer = core.newer_launcher()
            if not newer:
                return
            core.log(f"Actualizando el launcher a {newer[0]}")
            self.emit(type="selfupdate_start")
            time.sleep(0.6)
            core.apply_update(newer[1], me, lambda fr: self.emit(type="selfupdate", frac=fr))
            subprocess.Popen([me], cwd=os.path.dirname(me), env=gl_selfcheck.clean_env())
            self._window.destroy()
        except Exception as e:  # noqa: BLE001
            core.log("No se pudo actualizar el launcher: " + traceback.format_exc())
            self.emit(type="toast", text="No se pudo actualizar el launcher: " + str(e), err=True)

    # ---------------------------------------------------------- ventana
    def is_demo(self):
        return self._demo

    def demo_state(self):
        return os.environ.get("GL_DEMO_STATE", "")

    def client_info(self, d):
        """Datos de pantalla que manda la interfaz (para depurar tamanos)."""
        try:
            core.log(f"viewport: {json.dumps(d)} ventana={self._window.width}x{self._window.height}")
        except Exception:  # noqa: BLE001
            pass

    def minimize(self):
        self._window.minimize()

    def close(self):
        self._window.destroy()

    def pick_folder(self, initial=None):
        res = self._window.create_file_dialog(webview.FOLDER_DIALOG, directory=os.path.dirname(initial or "") or "")
        if not res:
            return None
        d = os.path.normpath(res[0])
        return d if os.path.basename(d).lower() == "gitanoland" else os.path.join(d, "GitanoLand")

    def open_path(self, p):
        if p and os.path.exists(p):
            core.open_in_system(p)

    def open_url(self, u):
        webbrowser.open(u)

    def open_logs(self):
        os.makedirs(core.LOG_DIR, exist_ok=True)
        core.open_in_system(core.LOG_DIR)

    # ---------------------------------------------------------- instalar en el sistema
    def install_system(self):
        try:
            dst = core.install_to_system(True)
            self.save_cfg(skip_install=False)
            subprocess.Popen([dst], cwd=os.path.dirname(dst), env=gl_selfcheck.clean_env())
            threading.Timer(0.5, self._window.destroy).start()
            return {"ok": True}
        except Exception as e:  # noqa: BLE001
            core.log("install_system: " + traceback.format_exc())
            return {"ok": False, "error": str(e)}

    def skip_system(self):
        self.save_cfg(skip_install=True)
        return True

    # ---------------------------------------------------------- instalacion / actualizacion
    def start_install(self, w, update=False):
        threading.Thread(target=self._install, args=(w, update), daemon=True).start()

    def _install(self, w, update):
        def mark(i):
            self.emit(type="step", i=i)

        def log(t):
            core.log(t)
            self.emit(type="log", text=t)

        def prog(fr):
            self.emit(type="progress", frac=fr)
        try:
            mode, profile, variant = w["mode"], w["profile"], w["variant"]
            ram = float(w["ram"])
            full = core.is_full(mode)
            key = state_key(mode, profile, variant)
            core.log(f"Instalar: {mode} {profile} {variant} ram={ram} base={w['base']} update={update} reinstall={w.get('reinstall')}")
            mark(0)
            if os.path.normcase(w["base"]) != os.path.normcase(core.base_dir()):
                if self._cfg.get("installed"):
                    core.move_base(w["base"], log, prog)
                else:
                    core.BASE[0] = w["base"]
            if full:
                core.CUR[0] = mode
                core.ensure_prism(log, prog)
                core.install_icon()
                dest = core.instance_dir(profile, variant)
            else:
                dest = self._other_dest(mode, profile, variant)
            mark(1)
            self.emit(type="busy")
            rel = core.latest_release(variant)
            if w.get("reinstall"):
                st = core.load_state()
                st.pop(key, None)
                core.save_state(st)
            installed = core.load_state().get(key, {}).get("tag")
            if installed != rel["tag_name"]:
                core.sync(key, profile, rel, dest, log, prog, mode, variant)
            mark(2)
            javaw = None
            if variant == "java25":
                javaw = core.ensure_jdk25(log, prog)
                if full:
                    core.patch_java(profile, variant, javaw)
            if full:
                core.set_ram(profile, variant, ram)
            else:
                core.ensure_neoforge(log)
            mark(3)
            self.save_cfg(installed=True, kind=w["kind"], mode=mode, profile=profile, variant=variant, ram=ram,
                          nick=w.get("nick", ""), base=core.base_dir(), dest=dest, javaw=javaw or "")
            time.sleep(0.4)
            cfg = dict(self._cfg)
            cfg["tag"] = core.load_state().get(key, {}).get("tag", "")
            self.emit(type="done", cfg=cfg)
        except Exception as e:  # noqa: BLE001
            core.log("ERROR en la instalacion:" + chr(10) + traceback.format_exc())
            self._last_error = str(e)
            self.emit(type="fail", message=str(e), tail=core.anonymize(core.log_tail(25)))

    def _other_dest(self, mode, profile, variant):
        if mode == "curseforge":
            inst = core.curseforge_instances()
            if inst:
                for d in sorted(os.listdir(inst)):
                    if "gitano" in d.lower():
                        return os.path.join(inst, d)
        return os.path.join(core.base_dir(), f"Juego-{profile}" + ("-J25" if variant == "java25" else ""))

    def pending_update(self, w):
        key = state_key(w["mode"], w["profile"], w["variant"])
        installed, latest = core.pending_update(key, w["variant"])
        return {"installed": installed, "latest": latest}

    # ---------------------------------------------------------- jugar
    def launch(self, w):
        try:
            core.CUR[0] = w["mode"]
            if not core.is_full(w["mode"]):
                return {"error": "Este launcher se abre a mano (mira la guia)."}
            accounts = os.path.exists(os.path.join(core.fl()["dir"], "accounts.json"))
            cmd = [core.prism_exe(), "--launch", core.instance_id(w["profile"], w["variant"])]
            if w["kind"] == "nopremium" and w["mode"] == "freesm" and w.get("nick"):
                cmd += ["--offline", w["nick"]]
            elif not accounts:
                return {"need_login": True}
            core.log("Lanzar: " + " ".join(cmd))
            subprocess.Popen(cmd, cwd=core.fl()["dir"], env=gl_selfcheck.child_env())
            return {"ok": True}
        except Exception as e:  # noqa: BLE001
            core.log("launch: " + traceback.format_exc())
            return {"error": str(e), "tail": core.anonymize(core.log_tail(20))}

    # ---------------------------------------------------------- guias paso a paso
    def login_guide(self, w):
        core.CUR[0] = w["mode"]
        name = core.fl()["name"]
        exe, cwd = core.prism_exe(), core.fl()["dir"]
        threading.Thread(target=lambda: subprocess.Popen([exe], cwd=cwd, env=gl_selfcheck.child_env()), daemon=True).start()
        return {"title": "Inicia sesión con tu cuenta", "steps": [
            {"t": f"Se abre {name}", "b": f"Ahora se abre {name}. Espera a que cargue su ventana."},
            {"t": "Inicia sesión", "b": "Arriba a la derecha pulsa tu cuenta > «Gestionar cuentas» > «Añadir Microsoft» y entra con tu cuenta. Solo tienes que hacerlo una vez."},
            {"t": "¡A jugar!", "b": f"Cierra {name} y pulsa Jugar aquí otra vez."}]}

    def guide(self, w):
        mode, ram, dest = w["mode"], w["ram"], self._cfg.get("dest", "")
        ver, ram_txt = f"NeoForge {core.NEOFORGE}", f"{ram:g} GB"
        java = []
        if w["variant"] == "java25":
            jw = self._cfg.get("javaw", "")
            java = [{"t": "Java 25", "b": "En los Ajustes del launcher pon esta ruta de Java:", "copy": jw}]
        folder = {"copy": dest, "open": dest if os.path.isdir(dest) else ""}
        if mode == "tlauncher":
            steps = [
                {"t": "Abre TLauncher", "b": "Ábrelo como siempre. Cuando veas su ventana principal, sigue."},
                {"t": "Elige la versión", "b": f"Abajo a la izquierda hay una lista de versiones. Escoge «{ver}». Si no sale, marca la casilla «Mostrar versiones instaladas»."},
                {"t": "Carpeta del juego", "b": "Pulsa el engranaje (Ajustes) y busca «Directorio del juego» (Game directory). Pega esta ruta:", **folder},
                {"t": "Memoria", "b": f"En esos mismos Ajustes busca «Memoria RAM» y ponla en {ram_txt}."}] + java + [
                {"t": "¡A jugar!", "b": "Pulsa JUGAR en TLauncher. Para actualizar, abre este launcher."}]
            return {"title": "Guía de TLauncher", "steps": steps}
        if mode == "curseforge":
            steps = [
                {"t": "Abre CurseForge", "b": "Abre la app y entra en Minecraft."},
                {"t": "Crea el perfil", "b": f"Pulsa «Crear perfil personalizado». Nombre: GitanoLand. Minecraft 1.21.1 y cargador NeoForge {core.NEOFORGE}. Dale a Crear (no hace falta ejecutarlo)."},
                {"t": "Vuelve aquí", "b": "Vuelve a este launcher y pulsa «Reinstalar» para que rellene el perfil que acabas de crear."},
                {"t": "Memoria", "b": f"En los ajustes del perfil pon la RAM en {ram_txt}."}] + java + [
                {"t": "¡A jugar!", "b": "Juega desde CurseForge."}]
            return {"title": "Guía de CurseForge", "steps": steps}
        return {"title": "Guía de tu launcher", "steps": [
            {"t": "Versión", "b": f"Tu launcher tiene que usar Minecraft 1.21.1 con {ver}."},
            {"t": "Carpeta del juego", "b": "Ponla como carpeta del juego (donde están mods y config):", **folder},
            {"t": "Memoria", "b": f"Pon {ram_txt} de RAM."}] + java + [{"t": "¡A jugar!", "b": "Abre el juego desde tu launcher."}]}

    # ---------------------------------------------------------- borrar
    def delete_instance(self, w, also_launcher=False):
        try:
            core.CUR[0] = w["mode"]
            key = state_key(w["mode"], w["profile"], w["variant"])
            dest = self._cfg.get("dest") or core.instance_dir(w["profile"], w["variant"])
            base = core.base_dir()
            core.log(f"Borrar instancia: {dest} (launcher tambien: {also_launcher})")
            # solo se borra dentro de la carpeta de instalacion del launcher (nunca una carpeta ajena)
            if dest and os.path.isdir(dest) and os.path.normcase(os.path.abspath(dest)).startswith(os.path.normcase(os.path.abspath(base))):
                shutil.rmtree(dest, ignore_errors=True)
            st = core.load_state()
            st.pop(key, None)
            if also_launcher and core.is_full(w["mode"]) and os.path.isdir(core.fl()["dir"]):
                shutil.rmtree(core.fl()["dir"], ignore_errors=True)
            self._cfg.update(installed=False)
            st["_config"] = self._cfg
            core.save_state(st)
            return {"ok": True, "cfg": dict(self._cfg)}
        except Exception as e:  # noqa: BLE001
            core.log("delete: " + traceback.format_exc())
            return {"ok": False, "error": str(e)}

    # ---------------------------------------------------------- informe de errores
    def make_report(self, error=""):
        err = core.anonymize(error or self._last_error or "(sin mensaje)")
        tail = core.anonymize(core.log_tail(60))
        body = (f"**Launcher:** {core.LAUNCHER_VERSION}\n**Sistema:** {core.os_label()}\n"
                f"**Modo:** {self._cfg.get('mode')} / {self._cfg.get('profile')} / {self._cfg.get('variant')}\n\n"
                f"**Error:**\n```\n{err}\n```\n\n<details><summary>Registro</summary>\n\n```\n{tail}\n```\n</details>\n")
        return {"text": body, "url": core.issue_url("Error en el launcher: " + err.splitlines()[0][:70], body), "path": core.log_path()}


_MUTEX_KEEP = []


def _already_running_posix(demo=False):
    """Linux: un fichero de bloqueo (flock); lo suelta solo el sistema al cerrarse el proceso."""
    import fcntl
    os.makedirs(core.HOME, exist_ok=True)
    f = open(os.path.join(core.HOME, "launcher-demo.lock" if demo else "launcher.lock"), "w")
    try:
        fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        return True
    _MUTEX_KEEP.append(f)
    return False


def already_running(demo=False):
    """Solo una ventana a la vez: si ya hay otra abierta, la trae al frente y esta se cierra."""
    if not core.IS_WIN:
        return _already_running_posix(demo)
    import ctypes
    k32, u32 = ctypes.windll.kernel32, ctypes.windll.user32
    _MUTEX_KEEP.append(k32.CreateMutexW(None, False, ("GitanoLandLauncherDemo" if demo else "GitanoLandLauncherSingleton")))
    if k32.GetLastError() != 183:  # ERROR_ALREADY_EXISTS
        return False
    hwnd = u32.FindWindowW(None, "GitanoLand (demo)" if demo else "GitanoLand")
    if hwnd:
        if u32.IsIconic(hwnd):
            u32.ShowWindow(hwnd, 9)  # SW_RESTORE
        u32.ShowWindow(hwnd, 5)
        u32.SetForegroundWindow(hwnd)
    return True


def main():
    demo = "--demo" in sys.argv
    if already_running(demo):
        return
    api = Api(demo)
    ui = core.resource(os.path.join("ui", "index.html"))
    api._window = webview.create_window("GitanoLand (demo)" if demo else "GitanoLand", ui, js_api=api, width=1060, height=680, frameless=True, easy_drag=False,
                                       resizable=False, shadow=True, background_color="#0c0d10")
    try:
        webview.start(debug=False, private_mode=False, storage_path=os.environ.get("GL_WEBVIEW_DIR") or os.path.join(core.HOME, "webview-demo" if demo else "webview"))
    except Exception:  # noqa: BLE001
        core.log("webview: " + traceback.format_exc())
        raise


if __name__ == "__main__":
    main()
