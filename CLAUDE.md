# GitanoLand (mod de NeoForge 1.21.1)

Mod de Endo (modid `gitanoland`, paquete `com.endo.gitanoland`). Responde siempre en español, directo y conciso.

## Qué es
- **Kirky**: la moneda del mod (stack de 64, 100 al entrar por primera vez, 3 bolsas para guardarlos con 3 estados visuales: vacía, con monedas, llena/cerrada).
- **Tiendas con NPC que cobran en Kirkys**: Ammo Maker y Gunsmith (aldeanos con profesión propia que reclaman las mesas de TaCZ) y WeedsGuy (Wandering Trader que vende cosas de Smokeleaf). Pantalla de tienda propia, con colores por tienda (Gunsmith gris, Ammo Maker rojo y gris, WeedsGuy verde).
- Pegar a un aldeano-tienda puede subirle los precios (nunca bajarlos); se ve el precio normal tachado. Los NPC pueden morir.
- **Integración con Jackpot**: las tragaperras se configuran solas con Kirkys.
- **Dependencias obligatorias**: TaCZ (`tacz`) y Smokeleaf (`smokeleafindustries`). Jackpot es opcional.
- **Plugins de REI/JEI** (opcionales) que muestran los tratos de las tiendas, y categorías de REI para las recetas de Smokeleaf.

## Estructura (src/main/java/com/endo/gitanoland)
- `GitanoLand.java` registra items, huevos, tab, componente de datos de las bolsas, configs y compat.
- `Config.java` (servidor, `gitanoland-server.toml`) y `ClientConfig.java` (cliente, fuerza del efecto de Smokeleaf).
- `item/` bolsas y huevos; `shop/` menú, ofertas, tablas de tratos, cartera y recargo (`Markup`); `client/ShopScreen` (pantalla), `client/StonedBoost`.
- `villager/` profesiones y POI; `entity/` WeedsGuy; `event/` eventos; `command/GitanoCommands` (`/gitanoland tacz_ids` y `/gitanoland poi`).
- `compat/` Jackpot, `compat/viewer` (datos comunes), `compat/rei`, `compat/jei`.
- Tratos: `src/main/resources/data/gitanoland/trades/*.json`. Se copian a `config/gitanoland/trades/` la primera vez y **esa copia manda sobre la del jar**: si se cambian los JSON, hay que borrar esa carpeta en la instancia.

## Compilar y probar
La lista de jars de `libs/` y de dónde bajarlos está en `LIBS.md`. En Windows (PowerShell):
```powershell
$env:GRADLE_USER_HOME="D:\gradle-home"   # la caché de Gradle en D: (C: casi lleno)
.\gradlew build                          # el jar sale en build\libs\
.\gradlew runClient                       # cliente de desarrollo
```
- JDK 21. En PowerShell se usa `.\gradlew`; en Linux/Fedora, `./gradlew` y `export GRADLE_USER_HOME=...`.
- `libs/` contiene los jars de los mods. Solo se cargan en desarrollo los que contienen `jackpot`, `tacz` o `smokeleaf` (y se excluyen los que contienen `addon` o `tweaks`). REI, JEI, Architectury y Cloth Config solo se usan para compilar los plugins; si no hay jar de REI/JEI, su plugin no se compila.
- **Sodium e Iris no funcionan en `runClient`** (las clases del jar-in-jar no se encuentran). Están en `libs-opt/` para que no rompan el arranque. Para probar con shaders: meter el jar de `build\libs` en el launcher normal.

## Reglas de trabajo
- Antes de dar algo por hecho, compila. No inventes ids: los ids de armas de TaCZ de la instalación de Endo están en `config/gitanoland/tacz_ids.txt` (se genera con `/gitanoland tacz_ids`). Si faltan, vienen de un gun pack.
- No añadir texturas elaboradas: las texturas son provisionales (Endo las sustituye él).
- No tocar `libs/`, `libs-opt/` ni `build/`.
- **Archivos fuera de la carpeta del proyecto: preguntar antes.** No mirar el contenido de otras carpetas del PC salvo que sea imprescindible. Única excepción permitida por Endo: copiar el jar a la instancia de Prism `D:\Files Program\PrismLauncher\Instances\GitanoLand\minecraft\mods\` (solo ahí; el jar viejo se guarda como `.bak`).

## Versiones
- Nomenclatura: ALPHA X.Y, luego BETA X.Y. **`mod_version` debe empezar por número** (NeoForge rechaza `alpha-1.0` con "Illegal version number" y el juego crashea): ALPHA 1.0 = `1.0.0-alpha`, BETA 1.0 = `1.0.0-beta`. Ramas y tags sí pueden llamarse `alpha-1.0`. El jar sale como `gitanoland-<mod_version>.jar`.
- Historial: pre-alpha 1.2 (anterior) -> **ALPHA 1.0 = `1.0.0-alpha`** (actual, desde 2026-11-10; F5 verificado por Endo).
- Al subir de versión, el jar nuevo cambia de nombre: antes de copiarlo a Prism hay que quitar el jar viejo de `mods/` (mover a `.bak`) para no duplicar el mod.

## Estado / pendiente
- Hecho: efectos de Smokeleaf con shaders (arreglado), precios (se quedan los actuales, a Endo le gustan) y aldeanos que reclaman las mesas de TaCZ.
- **Pendiente:** que los plugins de REI/JEI muestren los crafteos de Smokeleaf (categorías de REI se escribieron sin poder compilar; verificar que compilan y se ven bien). `/gitanoland poi` dice el tipo de POI del bloque que miras.

## Notas de sesión (para retomar en otro dispositivo)
- Para que el progreso viaje entre dispositivos, el estado va en este archivo (se sincroniza con git), no en la memoria local de Claude.
- `libs/` y `libs-opt/` están en `.gitignore`: en un PC nuevo hay que volver a bajar los jars (enlaces en `LIBS.md`).
- `build.gradle` carga UN solo visor en `runClient` (REI y JEI juntos no se pueden): por defecto REI (+ Architectury y Cloth Config); con `.\gradlew runClient -Pjei` carga JEI. El otro solo se usa para compilar su plugin.
- Smokeleaf trae su propio plugin de JEI (joint, blunt, cepas, gummies...) pero no de REI. Por eso `compat/rei/SmokeleafRei` replica lo que falta: categorías de máquinas con una receta por cepa (grinder, extractor, drying con `dry_bud`, gummy machine), Joint y Blunt en rejilla 3x3 (`SmokeGridCategory`), bolsas, y `registerEntries` para que las variantes por cepa salgan en el buscador. Las cepas salen de `StrainRegistry.ids()` y las variantes de `StrainItemFactory.create(item, id)`.
- Probado 2026-11-10: Sodium+Iris siguen sin arrancar en `runClient` (ClassNotFoundException en un mixin de Sodium, aunque se extraigan los jar-in-jar). No insistir: los shaders se prueban con el jar de `build\libs` en el launcher normal.
- Prueba en curso: ver en REI (runClient) Joint, Blunt, gummies y buds por cepa. Compila; sin confirmar visualmente todavía. El plugin de JEI de GitanoLand solo cubre las tiendas, no los crafteos de Smokeleaf (eso lo hace el JEI propio de Smokeleaf).
- Endo prueba él mismo el cliente: al lanzar `runClient`, no cerrarlo.
- Para probar shaders: `.\gradlew build` y copiar `build\libs\gitanoland-<versión>.jar` a `D:\Files Program\PrismLauncher\Instances\GitanoLand\minecraft\mods\` (esa instancia ya lleva Iris, Sodium y REI). La copia anterior queda como `gitanoland-1.0.0.jar.bak`.
- Efecto Stoned y F5 (arreglado en `client/StonedBoost`, sin verificar en juego): vanilla cierra el post-proceso al cambiar de cámara y `StonedShaderManager` de Smokeleaf no lo recarga porque guarda `activeTier`. Si falta el efecto con Stoned activo, se pone `activeTier` a null por reflexión y Smokeleaf lo recarga en su siguiente tick.

## Ramas y releases
- `main`: solo releases buenas. `alpha` (y luego `beta`): desarrollo de cada fase. Tags por versión (`alpha-1.0`...). Releases de GitHub solo para versiones buenas, con el jar de GitanoLand adjunto (nunca los jars de otros mods).
- 2026-11-10: rama `alpha` creada y subida con el commit de alpha-1.0. Aún no hay tag ni release.
- jeed (Just Enough Effect Descriptions, en la instancia de Endo): muestra efectos en REI/JEI/EMI. GitanoLand añade descripciones `effect.smokeleafindustries.<id>.description` (en_us y es_es) y `data/gitanoland/recipe/jeed_stoned.json` (receta `jeed:effect_provider` con condición `mod_loaded jeed`) que lista Joint, Blunt, Bong, Dab Rig y comestibles como proveedores de Stoned. Sin verificar en juego. Los demás efectos solo tienen descripción: sus proveedores dependen de la cepa.

## Balance (2026-11-10, solo análisis; nada aplicado aún)
- Catálogo de las 127 armas, munición y accesorios de la instancia de Endo: `docs/catalogo-tacz.md` (packs `tacz_default_gun` + `daffas`). `config/gitanoland/tacz_ids.txt` no existe en la instancia.
- TTK: en `config/tacz-server.toml` los multiplicadores base están a 1.0; en `config/tacztweaks.json` `bulletProtection` está en `false` (con `true`, Projectile Protection reduce balas; Protection normal no cuenta) y `balancing.playerDamage`/`playerHeadshot`/`armorIgnore` están a 1.0 (palancas para alargar el TTK contra jugadores sin tocar el daño a mobs).
- Idea de reparto: cada Gunsmith/Ammo Maker con una especialidad aleatoria al aparecer (pistolas, fusiles, escopetas+SMG, francotiradores/DMR, pesadas) y munición por calibre. +3 % a precios de armas. Sin implementar, pendiente de OK de Endo.
- Aviso: en `mods/` de la instancia hay dos jars de TaCZ (1.1.8-hotfix-r7 y 1.2.0).
