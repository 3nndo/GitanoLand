"""GitanoLand Launcher: elige Alto / Medio / Bajo y se encarga de todo.

- Descarga un Prism Launcher portable propio (no toca el que tengas instalado).
- Instala la instancia elegida bajandose SOLO lo que falta (lee el zip de la release de GitHub por rangos).
- Al abrir, mira si hay version nueva y pregunta si quieres instalarla (solo baja los archivos que cambian).
- Conserva tus ajustes (options.txt, configs que hayas tocado, etc.) al actualizar.
"""
import io
import json
import os
import struct
import subprocess
import sys
import threading
import time
import urllib.request
import zipfile
import zlib
import tkinter as tk
from tkinter import messagebox, ttk

REPO = "3nndo/GitanoLand"
PRISM_API = "https://api.github.com/repos/PrismLauncher/PrismLauncher/releases/latest"
HOME = os.path.join(os.environ.get("LOCALAPPDATA", os.path.expanduser("~")), "GitanoLand")
PRISM_DIR = os.path.join(HOME, "Prism")
STATE_FILE = os.path.join(HOME, "state.json")
PROFILES = {
    "Alto": "PC bueno: shaders y todos los efectos (8 GB de RAM o mas)",
    "Medio": "PC medio: shaders ligeros, sin extras pesados (6 GB de RAM)",
    "Bajo": "PC malillo: sin shaders ni extras pesados (4-5 GB de RAM)",
}
UA = {"User-Agent": "GitanoLand-Launcher"}
MODS_PREFIX = "minecraft/mods/"


def resource(name):
    base = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(base, name)


# ------------------------------------------------------------------ red
def http(url, headers=None, timeout=60):
    req = urllib.request.Request(url, headers={**UA, **(headers or {})})
    return urllib.request.urlopen(req, timeout=timeout)


def get_json(url):
    with http(url) as r:
        return json.load(r)


def get_range(url, start, end):
    """Bytes [start, end] (ambos incluidos); el final puede pasarse del tamano del archivo."""
    last = None
    for attempt in range(4):
        try:
            with http(url, {"Range": f"bytes={start}-{end}"}, timeout=180) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(1 + attempt)
    raise IOError(f"No se pudo descargar: {last}")


def get_tail(url, n):
    """Ultimos n bytes (el CDN de GitHub no admite rangos 'desde el final': se pide el tamano antes)."""
    with http(url, {"Range": "bytes=0-0"}, timeout=120) as r:
        size = int(r.headers["Content-Range"].split("/")[1])
        r.read()
    return get_range(url, max(0, size - n), size - 1)


# ------------------------------------------------------------------ zip remoto
class Entry:
    __slots__ = ("name", "crc", "csize", "usize", "offset", "method")

    def __init__(self, name, crc, csize, usize, offset, method):
        self.name, self.crc, self.csize, self.usize, self.offset, self.method = name, crc, csize, usize, offset, method


def read_central_directory(url):
    tail = get_tail(url, 65536)
    i = tail.rfind(b"PK\x05\x06")
    if i < 0:
        raise IOError("zip remoto invalido")
    _, _, _, _, total, cd_size, cd_off, _ = struct.unpack("<IHHHHIIH", tail[i:i + 22])
    if cd_off == 0xFFFFFFFF or total == 0xFFFF:
        raise IOError("zip64 no soportado")
    cd = get_range(url, cd_off, cd_off + cd_size - 1)
    entries, p = [], 0
    while p < len(cd) and cd[p:p + 4] == b"PK\x01\x02":
        (_, _, _, flags, method, _, _, crc, csize, usize, nlen, elen, clen, _, _, _, off) = struct.unpack(
            "<IHHHHHHIIIHHHHHII", cd[p:p + 46])
        name = cd[p + 46:p + 46 + nlen].decode("utf-8" if flags & 0x800 else "cp437")
        entries.append(Entry(name, crc, csize, usize, off, method))
        p += 46 + nlen + elen + clen
    return entries


def fetch_entries(url, entries, progress):
    """Entrega (entry, bytes) agrupando peticiones contiguas. 'entries' va ordenada por offset."""
    max_chunk, max_gap = 48 * 1024 * 1024, 512 * 1024
    total = sum(e.csize for e in entries) or 1
    done = 0
    i = 0
    while i < len(entries):
        j = i
        start = entries[i].offset
        end = start + 30 + len(entries[i].name.encode()) + entries[i].csize + 1024
        while j + 1 < len(entries):
            nxt = entries[j + 1]
            nxt_end = nxt.offset + 30 + len(nxt.name.encode()) + nxt.csize + 1024
            if nxt.offset - end > max_gap or nxt_end - start > max_chunk:
                break
            j += 1
            end = max(end, nxt_end)
        blob = get_range(url, start, end - 1)
        for e in entries[i:j + 1]:
            rel = e.offset - start
            nlen, elen = struct.unpack("<HH", blob[rel + 26:rel + 30])
            data = blob[rel + 30 + nlen + elen: rel + 30 + nlen + elen + e.csize]
            yield e, data
            done += e.csize
            progress(done / total)
        i = j + 1


def inflate(e, data):
    out = data if e.method == 0 else zlib.decompress(data, -15)
    if len(out) != e.usize or (zlib.crc32(out) & 0xFFFFFFFF) != e.crc:
        raise IOError(f"archivo corrupto: {e.name}")
    return out


def file_crc(path):
    crc = 0
    with open(path, "rb") as f:
        while True:
            b = f.read(1 << 20)
            if not b:
                return crc & 0xFFFFFFFF
            crc = zlib.crc32(b, crc)


# ------------------------------------------------------------------ estado
def load_state():
    try:
        with open(STATE_FILE, encoding="utf-8") as f:
            return json.load(f)
    except Exception:  # noqa: BLE001
        return {}


def save_state(state):
    os.makedirs(HOME, exist_ok=True)
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=1)


def instance_id(profile):
    return f"GitanoLand-{profile}"


def instance_dir(profile):
    return os.path.join(PRISM_DIR, "instances", instance_id(profile))


# ------------------------------------------------------------------ Prism
def prism_exe():
    return os.path.join(PRISM_DIR, "prismlauncher.exe")


def ensure_prism(log, progress):
    if os.path.exists(prism_exe()):
        return
    log("Descargando Prism Launcher (una sola vez)...")
    rel = get_json(PRISM_API)
    asset = next(a for a in rel["assets"]
                 if a["name"].startswith("PrismLauncher-Windows-MSVC-Portable-") and a["name"].endswith(".zip"))
    buf = io.BytesIO()
    with http(asset["browser_download_url"], timeout=120) as r:
        size = int(r.headers.get("Content-Length") or asset["size"])
        while True:
            b = r.read(1 << 18)
            if not b:
                break
            buf.write(b)
            progress(buf.tell() / size)
    os.makedirs(PRISM_DIR, exist_ok=True)
    with zipfile.ZipFile(buf) as z:
        z.extractall(PRISM_DIR)
    open(os.path.join(PRISM_DIR, "prismlauncher_portable.txt"), "a").close()
    if not os.path.exists(prism_exe()):
        raise IOError("No se encontro prismlauncher.exe tras descomprimir Prism")


def install_icon():
    icons = os.path.join(PRISM_DIR, "icons")
    os.makedirs(icons, exist_ok=True)
    with open(resource("icon.png"), "rb") as src, open(os.path.join(icons, "gitanoland.png"), "wb") as dst:
        dst.write(src.read())


def patch_instance_cfg(profile):
    path = os.path.join(instance_dir(profile), "instance.cfg")
    if not os.path.exists(path):
        return
    lines = [l for l in open(path, encoding="utf-8").read().splitlines() if not l.startswith("iconKey=")]
    i = lines.index("[General]") + 1 if "[General]" in lines else 0
    lines.insert(i, "iconKey=gitanoland")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


# ------------------------------------------------------------------ release
def latest_release():
    """Ultima release del mod (ignora las de herramientas como 'launcher-*')."""
    for rel in get_json(f"https://api.github.com/repos/{REPO}/releases?per_page=30"):
        if rel.get("draft") or rel["tag_name"].startswith("launcher"):
            continue
        return rel
    raise IOError("No hay releases del mod")


def profile_asset(rel, profile):
    suffix = f"-{profile}.zip"
    for a in rel["assets"]:
        if a["name"].startswith("GitanoLand-") and a["name"].endswith(suffix):
            return a
    raise IOError(f"La release {rel['tag_name']} no tiene el perfil {profile}")


def sync(profile, rel, log, progress):
    """Deja la instancia igual que el zip de la release, bajando solo lo que cambia."""
    asset = profile_asset(rel, profile)
    url = asset["browser_download_url"]
    state = load_state()
    prof = state.get(profile, {})
    installed = prof.get("files", {})  # ruta relativa -> crc con el que se instalo
    first_install = not prof
    dest = instance_dir(profile)
    log("Leyendo la lista de archivos de la version...")
    entries = [e for e in read_central_directory(url) if not e.name.endswith("/")]
    root = entries[0].name.split("/", 1)[0] + "/"
    remote = {e.name[len(root):]: e for e in entries if e.name.startswith(root)}

    todo = []
    for rel_path, e in remote.items():
        if rel_path == "instance.cfg" and not first_install:
            continue
        target = os.path.join(dest, *rel_path.split("/"))
        if os.path.exists(target):
            local_crc = file_crc(target) if os.path.getsize(target) == e.usize else None
            if local_crc == e.crc:
                continue  # ya esta
            old = installed.get(rel_path)
            if old is not None and old == e.crc:
                continue  # la release no lo cambio: respeta lo que hayas tocado tu
            if old is not None and not rel_path.startswith(MODS_PREFIX):
                if file_crc(target) != old:
                    continue  # lo cambiaste tu y la release tambien: gana lo tuyo (salvo mods)
        todo.append((rel_path, e))

    todo_entries = sorted((e for _, e in todo), key=lambda x: x.offset)
    names = {e.name: p for p, e in todo}
    mb = sum(e.csize for e in todo_entries) / 1e6
    log(f"Descargando {len(todo_entries)} archivos ({mb:.0f} MB)..." if todo_entries else "Ya estaba todo al dia.")
    os.makedirs(dest, exist_ok=True)
    for e, data in fetch_entries(url, todo_entries, progress):
        out = inflate(e, data)
        target = os.path.join(dest, *names[e.name].split("/"))
        os.makedirs(os.path.dirname(target), exist_ok=True)
        tmp = target + ".gltmp"
        with open(tmp, "wb") as f:
            f.write(out)
        os.replace(tmp, target)

    # mods que ya no estan en la version nueva
    removed = 0
    for rel_path in list(installed):
        if rel_path.startswith(MODS_PREFIX) and rel_path not in remote:
            p = os.path.join(dest, *rel_path.split("/"))
            if os.path.exists(p):
                os.remove(p)
                removed += 1
    if first_install:
        patch_instance_cfg(profile)
    install_icon()
    state[profile] = {"tag": rel["tag_name"], "files": {p: e.crc for p, e in remote.items()}}
    save_state(state)
    log(f"Listo: {len(todo_entries)} archivos nuevos o cambiados, {removed} mods quitados.")


# ------------------------------------------------------------------ interfaz
class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("GitanoLand")
        self.resizable(False, False)
        try:
            self.icon_img = tk.PhotoImage(file=resource("icon.png")).subsample(8, 8)
            self.iconphoto(True, self.icon_img)
        except Exception:  # noqa: BLE001
            pass
        self.configure(padx=18, pady=14)
        state = load_state()
        tk.Label(self, text="GitanoLand", font=("Segoe UI", 18, "bold")).pack(anchor="w")
        tk.Label(self, text="Elige como va tu PC y pulsa Jugar. Yo hago el resto.", font=("Segoe UI", 10)).pack(anchor="w", pady=(0, 8))
        last = next((p for p in PROFILES if p in state), "Medio")
        self.profile = tk.StringVar(value=last)
        for name, desc in PROFILES.items():
            inst = f"  [instalada: {state[name]['tag']}]" if name in state else ""
            tk.Radiobutton(self, text=f"{name}: {desc}{inst}", variable=self.profile, value=name,
                           font=("Segoe UI", 10), anchor="w", justify="left").pack(anchor="w", pady=1)
        self.button = tk.Button(self, text="Jugar", font=("Segoe UI", 12, "bold"), bg="#3FAE4B", fg="white", command=self.play)
        self.button.pack(fill="x", pady=(12, 6), ipady=4)
        self.bar = ttk.Progressbar(self, length=480, maximum=1.0)
        self.bar.pack(fill="x")
        self.status = tk.Label(self, text="", font=("Segoe UI", 9), anchor="w", justify="left", wraplength=480)
        self.status.pack(anchor="w", pady=(4, 0))

    def log(self, text):
        self.after(0, lambda: self.status.config(text=text))

    def progress(self, frac):
        self.after(0, lambda: self.bar.config(value=frac))

    def play(self):
        self.button.config(state="disabled")
        threading.Thread(target=self.work, args=(self.profile.get(),), daemon=True).start()

    def ask(self, title, text):
        result = {}
        ev = threading.Event()

        def run():
            result["v"] = messagebox.askyesno(title, text)
            ev.set()
        self.after(0, run)
        ev.wait()
        return result["v"]

    def work(self, profile):
        try:
            ensure_prism(self.log, self.progress)
            install_icon()
            installed_tag = load_state().get(profile, {}).get("tag")
            self.log("Buscando actualizaciones...")
            rel = None
            try:
                rel = latest_release()
            except Exception:  # noqa: BLE001
                if not installed_tag:
                    raise
                self.log("Sin conexion con GitHub: abro la version que ya tienes.")
            if rel is not None:
                if installed_tag is None:
                    sync(profile, rel, self.log, self.progress)
                elif installed_tag != rel["tag_name"]:
                    if self.ask("Hay una actualizacion",
                                f"Tienes {installed_tag} y ya esta {rel['tag_name']}.\n"
                                "¿Quieres instalarla ahora? (solo se bajan los archivos que cambian)"):
                        sync(profile, rel, self.log, self.progress)
            self.log("Abriendo Prism Launcher... (la primera vez tendras que iniciar sesion con tu cuenta de Minecraft)")
            subprocess.Popen([prism_exe(), "--launch", instance_id(profile)], cwd=PRISM_DIR)
            time.sleep(2)
            self.after(0, self.destroy)
        except Exception as e:  # noqa: BLE001
            msg = str(e)
            self.log(f"Error: {msg}")
            self.after(0, lambda: (self.button.config(state="normal"),
                                   messagebox.showerror("GitanoLand", f"Algo ha fallado:\n{msg}\n\nSi se repite, mandale una captura a Endo.")))


if __name__ == "__main__":
    App().mainloop()
