# Jars de `libs/` (no se redistribuyen: se descargan de Modrinth/CurseForge)

El build solo lee `libs/` (carpeta plana). Todo es NeoForge 1.21.1.

## Obligatorios (se cargan en desarrollo)
- `jackpot-1.0.8.jar` — [Modrinth](https://modrinth.com/mod/jackpot)
- `tacz-neoforge-1.21.1-1.2.0.jar` — [Modrinth](https://modrinth.com/mod/timeless-and-classics-zero)
- `smokeleafindustries-0.7.1-1.21.1.jar` — [CurseForge](https://www.curseforge.com/minecraft/mc-mods/smokeleaf-industry) (no está en Modrinth)

## Solo para compilar los plugins de REI/JEI (no se cargan en desarrollo)
- `RoughlyEnoughItems-16.0.799-neoforge.jar` — [Modrinth](https://modrinth.com/mod/rei)
- `architectury-13.0.11-neoforge.jar` — [Modrinth](https://modrinth.com/mod/architectury-api)
- `cloth-config-15.0.140-neoforge.jar` — [Modrinth](https://modrinth.com/mod/cloth-config)
- `jei-1.21.1-neoforge-19.70.0.469.jar` — [Modrinth](https://modrinth.com/mod/jei)

## `libs-opt/` (Sodium e Iris no funcionan en `runClient`, probado; no los lee el build)
- `iris-neoforge-1.8.14-beta.1+mc1.21.1.jar` — [Modrinth](https://modrinth.com/mod/iris)
- `sodium-mod-0.8.13.jar` — mod interno sacado de `sodium-neoforge-0.8.13+mc1.21.1.jar` ([Modrinth](https://modrinth.com/mod/sodium), en `META-INF/jarjar/`)
- `fabric-api-base`, `fabric-block-view-api-v2`, `fabric-renderer-api-v1`, `fabric-rendering-data-attachment-v1` — también de `META-INF/jarjar/` de Sodium

No hacen falta (el build los ignora): entityculling, ferritecore, kotlinforforge, YACL, tacz-tweaks y taczaddon.
- Para Iris también se extrajeron de su `META-INF/jarjar/`: `glsl-transformer-3.0.0-pre3.jar` y `jcpp-1.4.14.jar`.
