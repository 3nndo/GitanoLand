# -*- mode: python ; coding: utf-8 -*-
# PyInstaller (Linux, carpeta): la interfaz usa Qt WebEngine (PyQt6) en lugar de WebView2.
from PyInstaller.utils.hooks import collect_all

datas = [('ui', 'ui'), ('icon.png', '.')]
binaries = []
hiddenimports = ['qtpy', 'PyQt6.QtWebEngineCore', 'PyQt6.QtWebEngineWidgets', 'PyQt6.QtWebChannel', 'PyQt6.QtNetwork']
tmp_ret = collect_all('webview')
datas += tmp_ret[0]; binaries += tmp_ret[1]; hiddenimports += tmp_ret[2]

a = Analysis(['gitanoland_launcher.py'], pathex=[], binaries=binaries, datas=datas, hiddenimports=hiddenimports,
             hookspath=[], hooksconfig={}, runtime_hooks=[], excludes=['tkinter', 'PyQt5', 'PySide2', 'PySide6', 'gi'],
             noarchive=False, optimize=0)
pyz = PYZ(a.pure)
exe = EXE(pyz, a.scripts, [], exclude_binaries=True, name='GitanoLand-Launcher', debug=False, bootloader_ignore_signals=False,
          strip=False, upx=False, console=False)
coll = COLLECT(exe, a.binaries, a.datas, strip=False, upx=False, name='GitanoLand-Launcher')
