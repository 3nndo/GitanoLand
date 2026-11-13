# GitanoLand Launcher

Programa para jugadores: elige Alto / Medio / Bajo, pulsa Jugar y se encarga de todo (Prism portable propio, instancia, mods y actualizaciones).

- Datos en `%LOCALAPPDATA%\GitanoLand` (Prism, instancias y `state.json`).
- Lee los zips de la ultima release del mod por rangos HTTP y solo baja los archivos que faltan o cambian; respeta `options.txt` y configs que el jugador haya tocado.
- Ignora las releases cuyo tag empiece por `launcher`.
- Compilar: `pyinstaller --onefile --windowed --name GitanoLand-Launcher --icon icon.ico --add-data "icon.png;." gitanoland_launcher.py` (`icon.png` es la `tab_icon` del mod).
