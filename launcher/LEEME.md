# GitanoLand Launcher 2.0 (interfaz HTML)

Ventana sin bordes (pywebview + WebView2) con la interfaz en **HTML/CSS/JS normal** (`ui/`): cambia colores en `ui/style.css` (`:root`), textos y pantallas en `ui/app.js`, imágenes en `ui/img/` (`logo.png`, `premium.jpg` = creeper, `nopremium.jpg` = conejo con degradado naranja, `promo1..12.webp` = fondos promocionales que rotan). Para probar la interfaz sin Python: servir `ui/` con `python -m http.server` y abrir `index.html?mock` (usa `mock.js`; añade `&installed` y `&update` para ver la pantalla principal y el botón naranja).

Python (`gitanoland_launcher.py` = API que llama la interfaz; `gl_core.py` = red, zips, Prism/Freesm, instalación, log, RAM).

- Premium = Prism portable propio; No premium = Freesm portable propio (`--offline <nombre>`); otros (TLauncher, CurseForge...) = guía paso a paso.
- Asistente Tu PC > Ajustes (RAM 4/6/8/12/otro con aviso si pasa de lo que tiene el PC y "bajo tu responsabilidad", Java 21/25, nombre, carpeta) > Instalar. Pantalla principal: Jugar/Actualizar (naranja, ⋮ "Jugar sin actualizar"), Editar instancia, Reinstalar, Borrar.
- Una sola ventana a la vez (mutex): al abrir otra trae la primera al frente.
- Errores: se guarda un `.log` en `%LOCALAPPDATA%\GitanoLand\logs` y la pantalla de error ofrece copiar el informe, abrir el aviso de GitHub ya rellenado (rutas y usuario anonimizados) y abrir la carpeta del log.
- Actualizar el juego solo baja lo que cambia y nunca pisa `options.txt`, `optionsof.txt`, `optionsshaders.txt` ni `servers.dat`. Se instala en el sistema con accesos directos y se actualiza solo (release `launcher-X.Y.Z` más nueva; sube `LAUNCHER_VERSION` en `gl_core.py`).
- Compilar (carpeta con `gl_core.py`, `gitanoland_launcher.py`, `icon.png`, `icon.ico`, `ui/`; requiere `pip install pywebview`): `pyinstaller --noconfirm --onefile --windowed --name GitanoLand-Launcher --icon icon.ico --add-data "ui;ui" --add-data "icon.png;." --collect-all webview --hidden-import clr_loader gitanoland_launcher.py`
- Variable `GL_WEBVIEW_DIR` = carpeta de datos de WebView2 (útil para pruebas en paralelo).

## Demo privada (probar todos los estados sin descargar ni instalar nada)
`GitanoLand-Launcher-Demo.exe` (se compila con `demo_launcher.py`, igual que el comando de arriba pero con `demo_launcher.py` y `--name GitanoLand-Launcher-Demo`; **no se publica**). Backend simulado (`ui/mock.js`) y un panel «DEMO · estados» arriba a la derecha (se despliega al pulsarlo) con botones para saltar a cualquier pantalla: bienvenida, asistente, aviso de RAM, instalación simulada (con o sin error), pantalla principal (al día / actualización naranja / sin conexión), borrar, guías, etc. La ventana (minimizar, cerrar, arrastrar) es real. Variable `GL_DEMO_STATE="<nombre del botón>"` abre directamente un estado.
Modo bajo consumo: añadir `?lowfx` a la URL (navegador) desactiva el cristal y el movimiento del fondo.
Efecto liquid glass: clase `.glass` (y reglas del final de `style.css`) + filtro SVG `#liquid` de `index.html`.
