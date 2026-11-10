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
La lista de jars de `libs/` está en `LIBS.md`. En Windows (PowerShell):
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

## Pendiente / ideas
- **Efectos de Smokeleaf con shaders de Iris/Sodium**: el efecto usa `gameRenderer.loadEffect` (post-proceso de vanilla) y con un shaderpack no se ve. Opciones: apagar el shaderpack durante el efecto con la API de Iris (`IrisApi.getInstance().getConfig().setShadersEnabledAndApply`) o rehacer el efecto como overlay propio. Falta la carpeta `shaders/post` del jar de Smokeleaf para ver qué dibuja cada tier.
- Precios provisionales: silenciador Vulture 100, Wraith 140, PKM 1250, grinders 60/250. Gunsmith nivel 4 vacío.
- Comprobar que los aldeanos reclaman las mesas de TaCZ (`/gitanoland poi` dice el tipo de POI del bloque que miras).
- Verificar que el plugin de REI y las categorías de Smokeleaf compilan y se ven bien (se escribieron sin poder compilar).
