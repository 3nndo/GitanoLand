"""Entrada del exe de DEMO (privado): igual que el launcher pero con backend simulado y un panel para saltar entre estados."""
import sys

if "--demo" not in sys.argv:
    sys.argv.append("--demo")

import gitanoland_launcher  # noqa: E402

gitanoland_launcher.main()
