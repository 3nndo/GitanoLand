#!/bin/bash
# Se ejecuta DENTRO del contenedor (ver build-linux.ps1/README): /src = carpeta launcher, /out = salida.
set -euo pipefail
VER="${1:-2.0.1}"
rm -rf /build/work && mkdir -p /build/work && cp -r /src/. /build/work/ && cd /build/work
cp linux/GitanoLand-Launcher-linux.spec .
rm -rf linux
pyinstaller --noconfirm --clean --distpath /build/dist --workpath /build/pybuild GitanoLand-Launcher-linux.spec
APPDIR=/build/AppDir
rm -rf "$APPDIR" && mkdir -p "$APPDIR/usr/bin"
cp -r /build/dist/GitanoLand-Launcher/. "$APPDIR/usr/bin/"
cp icon.png "$APPDIR/gitanoland.png"
cat > "$APPDIR/gitanoland.desktop" <<DESK
[Desktop Entry]
Type=Application
Name=GitanoLand
Comment=Instala y actualiza GitanoLand
Exec=GitanoLand-Launcher
Icon=gitanoland
Categories=Game;
Terminal=false
DESK
cat > "$APPDIR/AppRun" <<'RUN'
#!/bin/bash
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/usr/bin/GitanoLand-Launcher" "$@"
RUN
chmod +x "$APPDIR/AppRun" "$APPDIR/usr/bin/GitanoLand-Launcher"
cd /build
if [ ! -x appimagetool ]; then
  curl -fsSL -o appimagetool https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x appimagetool
fi
ARCH=x86_64 ./appimagetool --appimage-extract-and-run --no-appstream "$APPDIR" "/out/GitanoLand-Launcher-$VER-x86_64.AppImage"
ls -la /out
