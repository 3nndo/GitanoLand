"""Proteccion del arranque del launcher empaquetado (PyInstaller onefile). Solo usa os/sys/subprocess.

Problema: los launchers viejos se actualizan bajando el exe nuevo encima y abriendolo con
subprocess.Popen([sys.executable]). El exe nuevo hereda las variables _PYI_* del viejo, se cree su "proceso hijo"
y usa la carpeta temporal (_MEI...) del launcher viejo en vez de descomprimir la suya: faltan modulos
(p. ej. "No module named 'select'") y se cierra.

Solucion: el exe lleva un sello gl_build.txt con su version. Si la carpeta en la que arranca no tiene NUESTRO sello,
se vuelve a abrir con el entorno limpio (PYINSTALLER_RESET_ENVIRONMENT=1) y este proceso se cierra.
"""
import os
import subprocess
import sys

BUILD = "2.0.1"


def clean_env():
    """Entorno sin las variables internas de PyInstaller, para abrir otro exe nuestro sin que herede nuestra carpeta."""
    env = {k: v for k, v in os.environ.items() if not k.upper().startswith(("_PYI_", "_MEI"))}
    env["PYINSTALLER_RESET_ENVIRONMENT"] = "1"
    env.pop("GL_RESPAWNED", None)
    return env


def ensure_own_bundle():
    if not getattr(sys, "frozen", False):
        return
    respawned = os.environ.pop("GL_RESPAWNED", None)
    try:
        with open(os.path.join(sys._MEIPASS, "gl_build.txt"), encoding="utf-8") as f:
            ok = f.read().strip() == BUILD
    except OSError:
        ok = False
    if ok or respawned:
        return
    env = clean_env()
    env["GL_RESPAWNED"] = "1"
    subprocess.Popen([sys.executable] + sys.argv[1:], cwd=os.path.dirname(sys.executable), env=env)
    os._exit(0)
