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
LAUNCHER_VERSION = "1.3.0"
PRISM_API = "https://api.github.com/repos/PrismLauncher/PrismLauncher/releases/latest"
HOME = os.path.join(os.environ.get("LOCALAPPDATA", os.path.expanduser("~")), "GitanoLand")
PRISM_DIR = os.path.join(HOME, "Prism")
STATE_FILE = os.path.join(HOME, "state.json")
VARIANTS = {
    "java21": "Java 21 (recomendado: el mas compatible)",
    "java25": "Java 25 (con Reimagined Intro; el launcher baja su propia Java 25)",
}
JDK25_URL = "https://aka.ms/download-jdk/microsoft-jdk-25.0.1-windows-x64.zip"
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


def instance_id(profile, variant="java21"):
    return f"GitanoLand-{profile}" + ("-J25" if variant == "java25" else "")


def instance_dir(profile, variant="java21"):
    return os.path.join(PRISM_DIR, "instances", instance_id(profile, variant))


def ensure_jdk25(log, progress):
    """Devuelve la ruta de javaw.exe de una Java 25 propia del launcher (la baja si hace falta)."""
    import glob
    root = os.path.join(HOME, "Java25")

    def find():
        hits = glob.glob(os.path.join(root, "*", "bin", "javaw.exe"))
        return hits[0].replace(chr(92), "/") if hits else None
    found = find()
    if found:
        return found
    log("Descargando Java 25 (una sola vez, unos 220 MB)...")
    os.makedirs(root, exist_ok=True)
    tmp = os.path.join(HOME, "jdk25.zip")
    with http(JDK25_URL, timeout=120) as r, open(tmp, "wb") as f:
        size = int(r.headers.get("Content-Length") or 220_000_000)
        done = 0
        while True:
            b = r.read(1 << 18)
            if not b:
                break
            f.write(b)
            done += len(b)
            progress(done / size)
    with zipfile.ZipFile(tmp) as z:
        z.extractall(root)
    os.remove(tmp)
    found = find()
    if not found:
        raise IOError("No se encontro javaw.exe tras descomprimir Java 25")
    return found


def patch_java(profile, variant, javaw):
    """Hace que la instancia Prism use esa Java y no comprueba la version."""
    path = os.path.join(instance_dir(profile, variant), "instance.cfg")
    if not os.path.exists(path):
        return
    drop = ("OverrideJavaLocation=", "JavaPath=", "IgnoreJavaCompatibility=", "AutomaticJava=", "JavaVersion=")
    lines = [l for l in open(path, encoding="utf-8").read().splitlines() if not l.startswith(drop)]
    i = lines.index("[General]") + 1 if "[General]" in lines else 0
    lines[i:i] = ["OverrideJavaLocation=true", "JavaPath=" + javaw, "IgnoreJavaCompatibility=true", "AutomaticJava=false"]
    nl = chr(10)
    with open(path, "w", encoding="utf-8", newline=nl) as f:
        f.write(nl.join(lines) + nl)


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


def patch_instance_cfg(profile, variant="java21"):
    path = os.path.join(instance_dir(profile, variant), "instance.cfg")
    if not os.path.exists(path):
        return
    lines = [l for l in open(path, encoding="utf-8").read().splitlines() if not l.startswith("iconKey=")]
    i = lines.index("[General]") + 1 if "[General]" in lines else 0
    lines.insert(i, "iconKey=gitanoland")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


# ------------------------------------------------------------------ auto-actualizacion del launcher
def vtuple(tag):
    nums = tag.split("launcher-", 1)[-1].lstrip("v").split("-")[0].split(".")
    return tuple(int(n) for n in nums if n.isdigit())


def newer_launcher():
    """(tag, url) de la release del launcher mas nueva que la actual, o None."""
    best = None
    for rel in get_json(f"https://api.github.com/repos/{REPO}/releases?per_page=30"):
        if rel.get("draft") or not rel["tag_name"].startswith("launcher-"):
            continue
        asset = next((a for a in rel["assets"] if a["name"].lower().endswith(".exe")), None)
        if asset and (best is None or vtuple(rel["tag_name"]) > vtuple(best[0])):
            best = (rel["tag_name"], asset["browser_download_url"])
    if best and vtuple(best[0]) > vtuple(LAUNCHER_VERSION):
        return best
    return None


def apply_update(url, exe, progress):
    """Baja el exe nuevo y lo deja en lugar del actual (Windows deja renombrar un exe en marcha, no borrarlo)."""
    new, old = exe + ".new", exe + ".old"
    with http(url, timeout=120) as r, open(new, "wb") as f:
        size = int(r.headers.get("Content-Length") or 0) or 1
        done = 0
        while True:
            b = r.read(1 << 18)
            if not b:
                break
            f.write(b)
            done += len(b)
            progress(done / size)
    if os.path.getsize(new) < 1_000_000:
        os.remove(new)
        raise IOError("La descarga del launcher salio incompleta")
    if os.path.exists(old):
        os.remove(old)
    os.replace(exe, old)
    os.replace(new, exe)


def cleanup_old_exe():
    if getattr(sys, "frozen", False):
        try:
            os.remove(sys.executable + ".old")
        except OSError:
            pass


# ------------------------------------------------------------------ otros launchers
NEOFORGE = "21.1.265"
NEOFORGE_INSTALLER = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE}/neoforge-{NEOFORGE}-installer.jar"


def appdata():
    return os.environ.get("APPDATA", os.path.expanduser("~"))


def minecraft_dir():
    return os.path.join(appdata(), ".minecraft")


def curseforge_instances():
    """Carpeta de perfiles de CurseForge (la de siempre, si existe)."""
    home = os.path.expanduser("~")
    for c in (os.path.join(home, "curseforge", "minecraft", "Instances"),
              os.path.join(home, "Documents", "curseforge", "minecraft", "Instances")):
        if os.path.isdir(c):
            return c
    return None


def find_java():
    import glob
    import shutil
    cands = []
    j = shutil.which("java")
    if j:
        cands.append(j)
    for pat in ("C:/Program Files/Java/*/bin/java.exe", "C:/Program Files/Eclipse Adoptium/*/bin/java.exe",
                "C:/Program Files/Microsoft/jdk*/bin/java.exe", "C:/Program Files/Zulu/*/bin/java.exe",
                os.path.join(appdata(), ".minecraft", "runtime", "*", "*", "*", "bin", "java.exe"),
                os.path.join(appdata(), ".tlauncher", "**", "bin", "java.exe")):
        cands += sorted(glob.glob(pat, recursive=True), reverse=True)
    for c in cands:
        try:
            out = subprocess.run([c, "-version"], capture_output=True, text=True, timeout=20).stderr
            ver = out.split('"')[1] if '"' in out else ""
            major = int(ver.split(".")[0]) if ver and ver.split(".")[0].isdigit() else 0
            if major >= 17:
                return c
        except Exception:  # noqa: BLE001
            continue
    return None


def ensure_neoforge(log):
    """Instala NeoForge en .minecraft (para TLauncher y similares) si no esta."""
    mc = minecraft_dir()
    if os.path.isdir(os.path.join(mc, "versions", f"neoforge-{NEOFORGE}")):
        return
    java = find_java()
    if not java:
        raise IOError("No encuentro Java 17 o superior. Instala Java 21 (adoptium.net), o abre una vez Minecraft desde tu launcher, y vuelve a probar.")
    log(f"Descargando NeoForge {NEOFORGE}...")
    os.makedirs(mc, exist_ok=True)
    installer = os.path.join(HOME, "neoforge-installer.jar")
    os.makedirs(HOME, exist_ok=True)
    with http(NEOFORGE_INSTALLER, timeout=120) as r, open(installer, "wb") as f:
        f.write(r.read())
    profiles = os.path.join(mc, "launcher_profiles.json")
    if not os.path.exists(profiles):
        with open(profiles, "w", encoding="utf-8") as f:
            json.dump({"profiles": {}, "version": 3}, f)
    log("Instalando NeoForge (tarda un par de minutos)...")
    res = subprocess.run([java, "-jar", installer, "--installClient", mc], capture_output=True, text=True,
                         creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    if res.returncode != 0 or not os.path.isdir(os.path.join(mc, "versions", f"neoforge-{NEOFORGE}")):
        tail = (res.stdout + res.stderr).strip().splitlines()[-4:]
        raise IOError("No se pudo instalar NeoForge:" + chr(10) + chr(10).join(tail))


# ------------------------------------------------------------------ release
def latest_release(variant="java21"):
    """Ultima release del mod de esa variante (ignora las de herramientas 'launcher-*')."""
    for rel in get_json(f"https://api.github.com/repos/{REPO}/releases?per_page=30"):
        tag = rel["tag_name"]
        if rel.get("draft") or tag.startswith("launcher"):
            continue
        if (variant == "java25") == tag.endswith("-java25"):
            return rel
    raise IOError("No hay releases del mod para " + variant)


def profile_asset(rel, profile):
    suffix = f"-{profile}.zip"
    for a in rel["assets"]:
        if a["name"].startswith("GitanoLand-") and a["name"].endswith(suffix):
            return a
    raise IOError(f"La release {rel['tag_name']} no tiene el perfil {profile}")


def sync(key, profile, rel, dest, log, progress, mode="prism", variant="java21"):
    """Deja la carpeta igual que el zip de la release, bajando solo lo que cambia.

    mode "prism": instancia completa (instance.cfg, mmc-pack.json y minecraft/). Otros: solo el contenido de minecraft/."""
    asset = profile_asset(rel, profile)
    url = asset["browser_download_url"]
    state = load_state()
    prof = state.get(key, {})
    installed = prof.get("files", {})  # ruta relativa -> crc con el que se instalo
    first_install = not prof
    log("Leyendo la lista de archivos de la version...")
    entries = [e for e in read_central_directory(url) if not e.name.endswith("/")]
    root = entries[0].name.split("/", 1)[0] + "/"
    remote = {e.name[len(root):]: e for e in entries if e.name.startswith(root)}
    mods_prefix = MODS_PREFIX
    if mode != "prism":
        remote = {k[len("minecraft/"):]: e for k, e in remote.items() if k.startswith("minecraft/")}
        mods_prefix = "mods/"

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
            if old is not None and not rel_path.startswith(mods_prefix):
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
        if rel_path.startswith(mods_prefix) and rel_path not in remote:
            p = os.path.join(dest, *rel_path.split("/"))
            if os.path.exists(p):
                os.remove(p)
                removed += 1
    if mode == "prism":
        if first_install:
            patch_instance_cfg(profile, variant)
        install_icon()
    state[key] = {"tag": rel["tag_name"], "dest": dest, "files": {p: e.crc for p, e in remote.items()}}
    save_state(state)
    log(f"Listo: {len(todo_entries)} archivos nuevos o cambiados, {removed} mods quitados.")


# ------------------------------------------------------------------ interfaz
LAUNCHERS = {
    "prism": "Prism Launcher (te lo preparo yo; necesita cuenta de Microsoft)",
    "curseforge": "CurseForge (la app de CurseForge)",
    "tlauncher": "TLauncher",
    "otro": "Otro launcher (te pregunto la carpeta del juego)",
}

CF_STEPS = (
    "Falta un paso en CurseForge, una sola vez:\n"
    "1. Abre CurseForge > Minecraft > Crear perfil personalizado.\n"
    "2. Nombre: GitanoLand. Minecraft 1.21.1 y cargador NeoForge 21.1.265.\n"
    "3. Dale a Crear (no hace falta ejecutarlo) y vuelve aqui a pulsar el boton.\n"
    "Luego en CurseForge, en los ajustes del perfil, pon la memoria RAM: {ram}.")
RAM = {"Alto": "6 GB", "Medio": "5 GB", "Bajo": "4 GB"}


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
        self.state_cfg = state.get("_config", {})
        tk.Label(self, text="GitanoLand  v" + LAUNCHER_VERSION, font=("Segoe UI", 18, "bold")).pack(anchor="w")
        tk.Label(self, text="1) Con que launcher juegas?", font=("Segoe UI", 10, "bold")).pack(anchor="w", pady=(6, 2))
        self.launcher = tk.StringVar(value=self.state_cfg.get("launcher", "prism"))
        for key, text in LAUNCHERS.items():
            tk.Radiobutton(self, text=text, variable=self.launcher, value=key, font=("Segoe UI", 10),
                           anchor="w", command=self.refresh).pack(anchor="w")
        tk.Label(self, text="2) Que Java usas?", font=("Segoe UI", 10, "bold")).pack(anchor="w", pady=(8, 2))
        self.variant = tk.StringVar(value=self.state_cfg.get("variant", "java21"))
        for vkey, text in VARIANTS.items():
            tk.Radiobutton(self, text=text, variable=self.variant, value=vkey, font=("Segoe UI", 10),
                           anchor="w", command=self.refresh).pack(anchor="w")
        tk.Label(self, text="3) Como va tu PC?", font=("Segoe UI", 10, "bold")).pack(anchor="w", pady=(8, 2))
        self.profile = tk.StringVar(value=self.state_cfg.get("profile", "Medio"))
        self.radios = {}
        for name, desc in PROFILES.items():
            r = tk.Radiobutton(self, text="", variable=self.profile, value=name, font=("Segoe UI", 10),
                               anchor="w", justify="left")
            r.pack(anchor="w", pady=1)
            self.radios[name] = (r, desc)
        self.button = tk.Button(self, text="Jugar", font=("Segoe UI", 12, "bold"), bg="#3FAE4B", fg="white", command=self.play)
        self.button.pack(fill="x", pady=(12, 6), ipady=4)
        self.bar = ttk.Progressbar(self, length=520, maximum=1.0)
        self.bar.pack(fill="x")
        self.status = tk.Label(self, text="", font=("Segoe UI", 9), anchor="w", justify="left", wraplength=520)
        self.status.pack(anchor="w", pady=(4, 0))
        self.refresh()
        threading.Thread(target=self.check_self_update, daemon=True).start()

    def check_self_update(self):
        try:
            found = newer_launcher()
        except Exception:  # noqa: BLE001
            return
        if not found:
            return
        tag, url = found
        if not getattr(sys, "frozen", False):
            return
        nl = chr(10)
        ver = tag.split("-", 1)[1]
        if not self.ask("Nueva version del launcher",
                        f"Hay una version nueva del launcher ({ver}) y tienes la {LAUNCHER_VERSION}." + nl +
                        "¿La instalo ahora? Se reinicia solo."):
            return
        try:
            self.after(0, lambda: self.button.config(state="disabled"))
            self.log("Actualizando el launcher...")
            apply_update(url, sys.executable, self.progress)
            subprocess.Popen([sys.executable])
            self.after(0, self.destroy)
        except Exception as e:  # noqa: BLE001
            msg = str(e)
            self.log(f"No se pudo actualizar el launcher: {msg}")
            self.after(0, lambda: self.button.config(state="normal"))

    def key_for(self, profile, mode=None, variant=None):
        mode = mode or self.launcher.get()
        variant = variant or self.variant.get()
        base = profile if mode == "prism" else f"{mode}:{profile}"
        return base + ("@java25" if variant == "java25" else "")

    def refresh(self):
        state = load_state()
        mode = self.launcher.get()
        for name, (radio, desc) in self.radios.items():
            tag = state.get(self.key_for(name, mode), {}).get("tag")
            radio.config(text=f"{name}: {desc}" + (f"  [instalada: {tag}]" if tag else ""))
        self.button.config(text="Jugar" if mode == "prism" else "Instalar / actualizar")

    def log(self, text):
        self.after(0, lambda: self.status.config(text=text))

    def progress(self, frac):
        self.after(0, lambda: self.bar.config(value=frac))

    def play(self):
        self.button.config(state="disabled")
        state = load_state()
        state["_config"] = {"launcher": self.launcher.get(), "profile": self.profile.get(), "variant": self.variant.get()}
        save_state(state)
        threading.Thread(target=self.work, args=(self.launcher.get(), self.profile.get(), self.variant.get()), daemon=True).start()

    def call(self, fn):
        """Ejecuta fn en el hilo de la interfaz y espera su resultado."""
        result, ev = {}, threading.Event()

        def run():
            try:
                result["v"] = fn()
            finally:
                ev.set()
        self.after(0, run)
        ev.wait()
        return result.get("v")

    def ask(self, title, text):
        return self.call(lambda: messagebox.askyesno(title, text))

    def info(self, title, text):
        self.call(lambda: messagebox.showinfo(title, text))

    def pick_dir(self, title, initial=None):
        from tkinter import filedialog
        return self.call(lambda: filedialog.askdirectory(title=title, initialdir=initial or os.path.expanduser("~")))

    # -------------------------------------------------------------- destinos
    def destination(self, mode, profile, key, state, variant="java21"):
        """Carpeta donde dejar los archivos (None si hay que parar)."""
        saved = state.get(key, {}).get("dest")
        if saved and os.path.isdir(saved):
            return saved
        if mode == "curseforge":
            inst = curseforge_instances()
            if not inst:
                self.info("CurseForge", "No encuentro la carpeta de perfiles de CurseForge.\n\n" + CF_STEPS.format(ram=RAM[profile]) +
                          "\n\nSi ya lo hiciste y no la encuentro, elige tu carpeta 'Instances' en la siguiente ventana.")
                inst = self.pick_dir("Carpeta 'Instances' de CurseForge")
                if not inst:
                    return None
            names = sorted(d for d in os.listdir(inst) if os.path.isdir(os.path.join(inst, d)))
            gl = [d for d in names if "gitano" in d.lower()]
            if not gl:
                self.info("CurseForge", CF_STEPS.format(ram=RAM[profile]))
                return None
            return os.path.join(inst, gl[0])
        if mode == "tlauncher":
            return os.path.join(HOME, f"Juego-{profile}" + ("-J25" if variant == "java25" else ""))
        folder = self.pick_dir("Carpeta del juego de tu launcher (donde estan 'mods' y 'config')")
        return folder or None

    def work(self, mode, profile, variant="java21"):
        try:
            state = load_state()
            key = self.key_for(profile, mode, variant)
            installed_tag = state.get(key, {}).get("tag")
            if mode == "prism":
                ensure_prism(self.log, self.progress)
                install_icon()
                dest = instance_dir(profile, variant)
            else:
                dest = self.destination(mode, profile, key, state, variant)
                if not dest:
                    self.log("Cancelado.")
                    self.after(0, lambda: self.button.config(state="normal"))
                    return
            self.log("Buscando actualizaciones...")
            rel = None
            try:
                rel = latest_release(variant)
            except Exception:  # noqa: BLE001
                if not installed_tag:
                    raise
                self.log("Sin conexion con GitHub: sigo con la version que ya tienes.")
            changed = False
            if rel is not None:
                if installed_tag is None:
                    sync(key, profile, rel, dest, self.log, self.progress, mode, variant)
                    changed = True
                elif installed_tag != rel["tag_name"]:
                    if self.ask("Hay una actualizacion",
                                f"Tienes {installed_tag} y ya esta {rel['tag_name']}.\n"
                                "¿Quieres instalarla ahora? (solo se bajan los archivos que cambian)"):
                        sync(key, profile, rel, dest, self.log, self.progress, mode, variant)
                        changed = True
            javaw = None
            if variant == "java25":
                javaw = ensure_jdk25(self.log, self.progress)
                if mode == "prism":
                    patch_java(profile, variant, javaw)
            if mode == "prism":
                self.log("Abriendo Prism Launcher... (la primera vez tendras que iniciar sesion con tu cuenta de Minecraft)")
                subprocess.Popen([prism_exe(), "--launch", instance_id(profile, variant)], cwd=PRISM_DIR)
                time.sleep(2)
                self.after(0, self.destroy)
                return
            if mode == "tlauncher":
                ensure_neoforge(self.log)
            self.after(0, lambda: self.bar.config(value=1.0))
            self.log("Listo.")
            self.info("GitanoLand", self.final_text(mode, profile, dest, changed, javaw))
            self.after(0, lambda: (self.refresh(), self.button.config(state="normal")))
        except Exception as e:  # noqa: BLE001
            msg = str(e)
            self.log(f"Error: {msg}")
            self.after(0, lambda: (self.button.config(state="normal"),
                                   messagebox.showerror("GitanoLand", f"Algo ha fallado:\n{msg}\n\nSi se repite, mandale una captura a Endo.")))

    @staticmethod
    def final_text(mode, profile, dest, changed, javaw=None):
        head = "Archivos instalados." if changed else "Todo estaba al dia."
        if javaw:
            head += chr(10) + chr(10) + "Usa esta Java 25 en tu launcher (ruta del ejecutable):" + chr(10) + javaw
        if mode == "curseforge":
            return f"{head}\n\nAbre CurseForge, entra en el perfil GitanoLand y dale a Jugar.\nMemoria recomendada: {RAM[profile]}."
        if mode == "tlauncher":
            return (f"{head}\n\nEn TLauncher:\n"
                    f"1. Elige la version 'NeoForge {NEOFORGE}' (si no sale, marca 'Mostrar versiones instaladas').\n"
                    "2. Ajustes > Directorio del juego (Game directory): ponlo en\n"
                    f"   {dest}\n"
                    f"3. Memoria RAM: {RAM[profile]}.\n"
                    "Y a jugar.")
        return (f"{head}\n\nLos archivos estan en:\n{dest}\n\nTu launcher debe usar Minecraft 1.21.1 con NeoForge {NEOFORGE}, "
                f"esa carpeta como carpeta del juego y {RAM[profile]} de RAM.")


if __name__ == "__main__":
    cleanup_old_exe()
    App().mainloop()
