# Launcher para Linux (AppImage)

El mismo launcher (interfaz HTML + `gl_core.py`) corre en Linux. Diferencias:
- La ventana usa **Qt WebEngine (PyQt6)** en vez de WebView2 (`PYWEBVIEW_GUI=qt`, y `--no-sandbox` porque el sandbox de Chromium no funciona en muchas distros dentro de un AppImage; solo carga la interfaz local).
- Datos en `~/.local/share/GitanoLand` (respeta `XDG_DATA_HOME`).
- Premium = **Prism** y No premium = **Freesm**, versiones `Linux-Qt6-Portable` (.tar.gz). Java 25 = Microsoft OpenJDK `linux-x64`.
- Un solo launcher abierto: `flock` en `~/.local/share/GitanoLand/launcher.lock`.
- Se actualiza solo: reemplaza el propio `.AppImage` (`$APPIMAGE`) con el de la release `launcher-*` mas nueva que tenga un `.AppImage`.
- No hay "instalar en el sistema": el AppImage ya es portable.

## Compilar (desde Windows con Docker Desktop, o en Linux)
Se compila en **Ubuntu 22.04** (glibc antigua = funciona en casi todas las distros).

    docker build -t gitanoland-linux-build launcher/linux
    docker run --rm -v <ruta>/launcher:/src:ro -v <salida>:/out gitanoland-linux-build bash /src/linux/build-appimage.sh 2.0.1

Sale `GitanoLand-Launcher-<version>-x86_64.AppImage` (~195 MB). Al subir de version: `gl_selfcheck.BUILD` y `gl_core.LAUNCHER_VERSION` (y el nombre del tag `launcher-X.Y.Z`).

## Probado
Dentro de Docker (Ubuntu 22.04, Xvfb): arranca la interfaz, descarga Prism/Freesm/Java 25, instala una instancia (Bajo, java21) y Freesm la abre. NO probado: un escritorio Linux real, Wayland, ni jugar de punta a punta.

## Requisitos del usuario
- x86_64. Ubuntu 22.04+ necesita `libfuse2` para abrir AppImages (`sudo apt install libfuse2`; en 24.04 `libfuse2t64`), o ejecutar con `--appimage-extract-and-run`.
- Para jugar, los paquetes que ya pide Prism (OpenGL, etc.).
