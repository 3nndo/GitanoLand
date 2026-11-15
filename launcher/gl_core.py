"""GitanoLand Launcher: elige Alto / Medio / Bajo y se encarga de todo.

- Descarga un Prism Launcher o Freesm Launcher portable propio (no toca el que tengas instalado). Freesm admite cuentas sin conexion.
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
import shutil

REPO = "3nndo/GitanoLand"
LAUNCHER_VERSION = "2.0.1"  # igual que gl_selfcheck.BUILD (el sello gl_build.txt del exe)
PRISM_API = "https://api.github.com/repos/PrismLauncher/PrismLauncher/releases/latest"
IS_WIN = sys.platform == "win32"
IS_LINUX = sys.platform.startswith("linux")
if IS_WIN:
    HOME = os.path.join(os.environ.get("LOCALAPPDATA", os.path.expanduser("~")), "GitanoLand")
else:  # Linux: ~/.local/share/GitanoLand (respeta XDG_DATA_HOME)
    HOME = os.path.join(os.environ.get("XDG_DATA_HOME") or os.path.join(os.path.expanduser("~"), ".local", "share"), "GitanoLand")
# Carpeta donde se instala todo (Prism/Freesm, instancias...). Por defecto HOME; el usuario puede cambiarla.
BASE = [HOME]


def base_dir():
    return BASE[0]
# Launchers de la familia Prism que el launcher prepara solo (instancia completa): Prism (cuenta de Microsoft)
# y Freesm (acepta cuentas sin conexion y de Microsoft).
# En Linux se baja el .tar.gz "Portable" (trae su propio Qt y un script PrismLauncher/FreesmLauncher en la raiz).
FLAVORS = {
    "prism": {"name": "Prism Launcher", "folder": "Prism", "api": PRISM_API,
              **({"exe": "prismlauncher.exe", "asset": "PrismLauncher-Windows-MSVC-Portable-", "marker": "prismlauncher_portable.txt", "ext": ".zip"}
                 if IS_WIN else {"exe": "PrismLauncher", "asset": "PrismLauncher-Linux-Qt6-Portable-", "marker": "portable.txt", "ext": ".tar.gz"})},
    "freesm": {"name": "Freesm Launcher", "folder": "Freesm",
               "api": "https://api.github.com/repos/FreesmTeam/FreesmLauncher/releases/latest",
               **({"exe": "freesmlauncher.exe", "asset": "FreesmLauncher-Windows-MSVC-Portable-", "marker": "portable.txt", "ext": ".zip"}
                  if IS_WIN else {"exe": "FreesmLauncher", "asset": "FreesmLauncher-Linux-Qt6-Portable-", "marker": "portable.txt", "ext": ".tar.gz"})},
}
CUR = ["prism"]  # launcher de la familia Prism en uso


def fl():
    f = dict(FLAVORS[CUR[0]])
    f["dir"] = os.path.join(base_dir(), f["folder"])
    return f


def is_full(mode):
    return mode in FLAVORS
STATE_FILE = os.path.join(HOME, "state.json")
VARIANTS = {
    "java21": "Java 21 (recomendado: el mas compatible)",
    "java25": "Java 25 (con Reimagined Intro; el launcher baja su propia Java 25)",
}
JDK25_URL = ("https://aka.ms/download-jdk/microsoft-jdk-25.0.1-windows-x64.zip" if IS_WIN
             else "https://aka.ms/download-jdk/microsoft-jdk-25.0.1-linux-x64.tar.gz")
JAVA_BIN = "javaw.exe" if IS_WIN else "java"
PROFILES = {
    "Alto": "PC bueno: shaders y todos los efectos (8 GB de RAM o mas)",
    "Medio": "PC medio: shaders ligeros, sin extras pesados (6 GB de RAM)",
    "Bajo": "PC malillo: sin shaders ni extras pesados (4-5 GB de RAM)",
}
UA = {"User-Agent": "GitanoLand-Launcher"}
MODS_PREFIX = "minecraft/mods/"
# Archivos del jugador que una actualizacion nunca pisa (sensibilidad, teclas, lista de servidores...).
KEEP_FILES = {p + n for p in ("", "minecraft/") for n in ("options.txt", "optionsof.txt", "optionsshaders.txt", "servers.dat")}


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
    return os.path.join(fl()["dir"], "instances", instance_id(profile, variant))


def ensure_jdk25(log, progress):
    """Devuelve la ruta de javaw.exe de una Java 25 propia del launcher (la baja si hace falta)."""
    import glob
    root = os.path.join(HOME, "Java25")

    def find():
        hits = glob.glob(os.path.join(root, "*", "bin", JAVA_BIN))
        return hits[0].replace(chr(92), "/") if hits else None
    found = find()
    if found:
        return found
    log("Descargando Java 25 (una sola vez, unos 220 MB)...")
    os.makedirs(root, exist_ok=True)
    tmp = os.path.join(HOME, "jdk25.zip" if IS_WIN else "jdk25.tar.gz")
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
    if IS_WIN:
        with zipfile.ZipFile(tmp) as z:
            z.extractall(root)
    else:
        extract_tar(tmp, root)
    os.remove(tmp)
    found = find()
    if not found:
        raise IOError("No se encontro " + JAVA_BIN + " tras descomprimir Java 25")
    return found


def extract_tar(src, dest, strip_top=False):
    """Descomprime un .tar.gz (ruta o fichero en memoria) conservando permisos. strip_top: quita una carpeta raiz unica."""
    import tarfile
    os.makedirs(dest, exist_ok=True)
    with (tarfile.open(src, "r:gz") if isinstance(src, str) else tarfile.open(fileobj=src, mode="r:gz")) as t:
        members = t.getmembers()
        tops = {m.name.split("/")[0] for m in members if m.name not in (".", "")}
        for m in members:
            if strip_top and len(tops) == 1 and m.name.split("/")[0] in tops and "/" in m.name:
                m.name = m.name.split("/", 1)[1]
            elif strip_top and len(tops) == 1 and m.name in tops and m.isdir():
                continue
            if m.name.startswith("/") or ".." in m.name.split("/"):
                continue
            t.extract(m, dest)


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
    return os.path.join(fl()["dir"], fl()["exe"])


def ensure_prism(log, progress):
    if os.path.exists(prism_exe()):
        return
    log(f"Descargando {fl()['name']} (una sola vez)...")
    rel = get_json(fl()["api"])
    asset = next(a for a in rel["assets"]
                 if a["name"].startswith(fl()["asset"]) and a["name"].endswith(fl()["ext"]))
    buf = io.BytesIO()
    with http(asset["browser_download_url"], timeout=120) as r:
        size = int(r.headers.get("Content-Length") or asset["size"])
        while True:
            b = r.read(1 << 18)
            if not b:
                break
            buf.write(b)
            progress(buf.tell() / size)
    os.makedirs(fl()["dir"], exist_ok=True)
    if fl()["ext"] == ".zip":
        with zipfile.ZipFile(buf) as z:
            z.extractall(fl()["dir"])
    else:
        buf.seek(0)
        extract_tar(buf, fl()["dir"], strip_top=True)
    open(os.path.join(fl()["dir"], fl()["marker"]), "a").close()
    if not os.path.exists(prism_exe()):
        raise IOError(f"No se encontro {fl()['exe']} tras descomprimir {fl()['name']}")


def install_icon():
    icons = os.path.join(fl()["dir"], "icons")
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


LAUNCHER_EXT = ".exe" if IS_WIN else ".appimage"


def self_path():
    """Archivo del launcher que se puede reemplazar al actualizar (Windows: el exe; Linux: el .AppImage). None si no se puede."""
    if not getattr(sys, "frozen", False):
        return None
    return sys.executable if IS_WIN else (os.environ.get("APPIMAGE") or None)


def newer_launcher():
    """(tag, url) de la release del launcher mas nueva que la actual, o None."""
    best = None
    for rel in get_json(f"https://api.github.com/repos/{REPO}/releases?per_page=30"):
        if rel.get("draft") or not rel["tag_name"].startswith("launcher-"):
            continue
        asset = next((a for a in rel["assets"] if a["name"].lower().endswith(LAUNCHER_EXT)), None)
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
    if not IS_WIN:
        os.chmod(new, 0o755)
    os.replace(exe, old)
    os.replace(new, exe)


def cleanup_old_exe():
    me = self_path()
    if me:
        try:
            os.remove(me + ".old")
        except OSError:
            pass


# ------------------------------------------------------------------ otros launchers
NEOFORGE = "21.1.265"
NEOFORGE_INSTALLER = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE}/neoforge-{NEOFORGE}-installer.jar"


def appdata():
    return os.environ.get("APPDATA", os.path.expanduser("~")) if IS_WIN else os.path.expanduser("~")


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
    for pat in (("/usr/lib/jvm/*/bin/java", "/opt/*/bin/java", os.path.join(appdata(), ".minecraft", "runtime", "*", "*", "bin", "java"))
                if not IS_WIN else ()):
        cands += sorted(glob.glob(pat), reverse=True)
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

    mode "prism" o "freesm": instancia completa (instance.cfg, mmc-pack.json y minecraft/). Otros: solo el contenido de minecraft/."""
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
    if not is_full(mode):
        remote = {k[len("minecraft/"):]: e for k, e in remote.items() if k.startswith("minecraft/")}
        mods_prefix = "mods/"

    todo = []
    for rel_path, e in remote.items():
        if rel_path == "instance.cfg" and not first_install:
            continue
        target = os.path.join(dest, *rel_path.split("/"))
        if rel_path in KEEP_FILES and os.path.exists(target):
            continue  # sensibilidad, teclas, servidores...: nunca se tocan una vez instalados
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
    if is_full(mode):
        if first_install:
            patch_instance_cfg(profile, variant)
        install_icon()
    state[key] = {"tag": rel["tag_name"], "dest": dest, "files": {p: e.crc for p, e in remote.items()}}
    save_state(state)
    log(f"Listo: {len(todo_entries)} archivos nuevos o cambiados, {removed} mods quitados.")




# ------------------------------------------------------------------ ajustes de la instancia, actualizaciones, instalacion
RAM_CHOICES = (4, 6, 8, 12)


def set_ram(profile, variant, gb):
    """Pone la memoria de la instancia (Prism/Freesm) en GB."""
    path = os.path.join(instance_dir(profile, variant), "instance.cfg")
    if not os.path.exists(path):
        return False
    mb = int(gb * 1024)
    drop = ("MaxMemAlloc=", "MinMemAlloc=", "OverrideMemory=")
    lines = [l for l in open(path, encoding="utf-8").read().splitlines() if not l.startswith(drop)]
    i = lines.index("[General]") + 1 if "[General]" in lines else 0
    lines[i:i] = ["OverrideMemory=true", f"MinMemAlloc={min(mb, 1024)}", f"MaxMemAlloc={mb}"]
    nl = chr(10)
    with open(path, "w", encoding="utf-8", newline=nl) as f:
        f.write(nl.join(lines) + nl)
    return True


def get_ram(profile, variant):
    """RAM (GB) que tiene la instancia ahora, o None."""
    path = os.path.join(instance_dir(profile, variant), "instance.cfg")
    try:
        for l in open(path, encoding="utf-8").read().splitlines():
            if l.startswith("MaxMemAlloc="):
                return round(int(l.split("=", 1)[1]) / 1024, 1)
    except Exception:  # noqa: BLE001
        pass
    return None


def pending_update(key, variant):
    """(tag instalado, tag mas nuevo) para saber si hay actualizacion; el mas nuevo es None sin conexion."""
    state = load_state()
    installed = state.get(key, {}).get("tag")
    try:
        latest = latest_release(variant)["tag_name"]
    except Exception:  # noqa: BLE001
        latest = None
    return installed, latest


def install_dir():
    return os.path.join(os.environ.get("LOCALAPPDATA", os.path.expanduser("~")), "Programs", "GitanoLand")


def installed_here():
    if not IS_WIN:
        return True  # el AppImage ya es portable: no hay instalacion en el sistema
    """True si el exe en marcha ya esta en la carpeta de instalacion del sistema."""
    if not getattr(sys, "frozen", False):
        return True  # en desarrollo no se instala nada
    return os.path.normcase(os.path.dirname(sys.executable)) == os.path.normcase(install_dir())


def desktop_dir():
    try:
        out = subprocess.run(["powershell", "-NoProfile", "-Command", "[Environment]::GetFolderPath('Desktop')"],
                             capture_output=True, text=True, timeout=20,
                             creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0)).stdout.strip()
        if out and os.path.isdir(out):
            return out
    except Exception:  # noqa: BLE001
        pass
    return os.path.join(os.path.expanduser("~"), "Desktop")


def make_shortcut(lnk, target, workdir, icon=None):
    cmd = ("$s=(New-Object -ComObject WScript.Shell).CreateShortcut('" + lnk.replace("'", "''") + "');"
           "$s.TargetPath='" + target.replace("'", "''") + "';$s.WorkingDirectory='" + workdir.replace("'", "''") + "';"
           + ("$s.IconLocation='" + icon.replace("'", "''") + "';" if icon else "") + "$s.Save()")
    subprocess.run(["powershell", "-NoProfile", "-Command", cmd], check=True, timeout=30,
                   creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))


def install_to_system(shortcuts=True):
    """Copia el launcher a %LOCALAPPDATA%/Programs/GitanoLand y crea accesos directos. Devuelve la ruta del exe nuevo."""
    dst_dir = install_dir()
    os.makedirs(dst_dir, exist_ok=True)
    dst = os.path.join(dst_dir, "GitanoLand-Launcher.exe")
    if getattr(sys, "frozen", False):
        shutil.copy2(sys.executable, dst)
    if shortcuts:
        make_shortcut(os.path.join(desktop_dir(), "GitanoLand.lnk"), dst, dst_dir)
        start = os.path.join(os.environ.get("APPDATA", ""), "Microsoft", "Windows", "Start Menu", "Programs")
        if os.path.isdir(start):
            make_shortcut(os.path.join(start, "GitanoLand.lnk"), dst, dst_dir)
    return dst


def move_base(new_base, log, progress):
    """Mueve todo lo instalado (Prism/Freesm/Juego-*) a otra carpeta y actualiza el estado."""
    old = base_dir()
    if os.path.normcase(old) == os.path.normcase(new_base):
        return
    os.makedirs(new_base, exist_ok=True)
    names = [n for n in os.listdir(old) if n in ("Prism", "Freesm") or n.startswith("Juego-")] if os.path.isdir(old) else []
    for i, n in enumerate(names):
        log(f"Moviendo {n}...")
        shutil.move(os.path.join(old, n), os.path.join(new_base, n))
        progress((i + 1) / max(1, len(names)))
    state = load_state()
    for k, v in state.items():
        if isinstance(v, dict) and isinstance(v.get("dest"), str) and v["dest"].startswith(old):
            v["dest"] = new_base + v["dest"][len(old):]
    state.setdefault("_config", {})["base"] = new_base
    save_state(state)
    BASE[0] = new_base


# ------------------------------------------------------------------ registro de errores y datos del PC
LOG_DIR = os.path.join(HOME, "logs")
_LOG = {"path": None}


def log_path():
    if _LOG["path"] is None:
        os.makedirs(LOG_DIR, exist_ok=True)
        _LOG["path"] = os.path.join(LOG_DIR, "launcher-" + time.strftime("%Y%m%d-%H%M%S") + ".log")
        # conserva solo los 10 ultimos
        old = sorted(f for f in os.listdir(LOG_DIR) if f.startswith("launcher-") and f.endswith(".log"))
        for f in old[:-9]:
            try:
                os.remove(os.path.join(LOG_DIR, f))
            except OSError:
                pass
    return _LOG["path"]


def log(msg):
    try:
        with open(log_path(), "a", encoding="utf-8") as f:
            f.write(time.strftime("[%H:%M:%S] ") + str(msg) + chr(10))
    except OSError:
        pass


def log_tail(n=40):
    try:
        return chr(10).join(open(log_path(), encoding="utf-8", errors="replace").read().splitlines()[-n:])
    except OSError:
        return ""


def anonymize(text):
    """Quita nombre de usuario y rutas personales del texto que se manda."""
    home = os.path.expanduser("~")
    user = os.path.basename(home)
    out = str(text).replace(home, "%USERPROFILE%")
    if user and len(user) > 2:
        out = out.replace(user, "<usuario>")
    return out


def ram_total_gb():
    """RAM fisica del PC en GB (0 si no se puede saber)."""
    if not IS_WIN:
        try:
            with open("/proc/meminfo", encoding="utf-8") as f:
                for line in f:
                    if line.startswith("MemTotal:"):
                        return round(int(line.split()[1]) / (1024 ** 2))
        except (OSError, ValueError):
            pass
        return 0
    try:
        import ctypes

        class MS(ctypes.Structure):
            _fields_ = [("dwLength", ctypes.c_ulong), ("dwMemoryLoad", ctypes.c_ulong), ("ullTotalPhys", ctypes.c_ulonglong),
                        ("ullAvailPhys", ctypes.c_ulonglong), ("ullTotalPageFile", ctypes.c_ulonglong),
                        ("ullAvailPageFile", ctypes.c_ulonglong), ("ullTotalVirtual", ctypes.c_ulonglong),
                        ("ullAvailVirtual", ctypes.c_ulonglong), ("ullAvailExtendedVirtual", ctypes.c_ulonglong)]
        m = MS()
        m.dwLength = ctypes.sizeof(MS)
        ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(m))
        return round(m.ullTotalPhys / (1024 ** 3))
    except Exception:  # noqa: BLE001
        return 0


def open_in_system(path):
    """Abre una carpeta o archivo con el programa por defecto del sistema."""
    if IS_WIN:
        os.startfile(path)
    else:
        import gl_selfcheck
        subprocess.Popen(["xdg-open", path], env=gl_selfcheck.child_env(), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def os_label():
    if IS_WIN:
        return "Windows " + (str(sys.getwindowsversion().major) if hasattr(sys, "getwindowsversion") else "?")
    try:
        with open("/etc/os-release", encoding="utf-8") as f:
            for line in f:
                if line.startswith("PRETTY_NAME="):
                    return "Linux - " + line.split("=", 1)[1].strip().strip('"')
    except OSError:
        pass
    return "Linux"


def issue_url(title, body):
    """Enlace a 'nuevo aviso' de GitHub con el informe ya rellenado (el usuario solo le da a enviar)."""
    from urllib.parse import quote
    return f"https://github.com/{REPO}/issues/new?title={quote(title)}&body={quote(body[:5500])}"
