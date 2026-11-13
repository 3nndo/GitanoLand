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
- **Desde 2026-11-12: fase pre-release.** `mod_version=1.0.0-pre.1` (rama `pre-release`, tag `v1.0.0-pre.N`). Cada commit anterior quedó como `alpha-0.1` ... `alpha-0.17` (tabla en `docs/versiones.md`). Las releases de GitHub no se pueden crear desde aquí (no hay `gh` ni conector): se hacen a mano con las notas de ese archivo.
- **2026-11-13: world border 15k** (solo servidor): `docs/world-border.md`; DimWorldBorder 1.1.1 copiado a `D:GitanoLand-servermods` (no se publica en releases por licencia ARR). Sin probar en servidor.
- **2026-11-13: dos lineas de pack.** Ramas `java21` (estandar, `v*-java21`, marcada latest) y `java25` (`v*-java25`, prerelease; anade Reimagined Intro 3.0.0, que pide Java 25 por el mixin JAVA_25). El codigo del mod es el mismo en ambas; solo cambia el pack de mods e `instance.cfg` (`IgnoreJavaCompatibility=true`). El launcher 1.3.0 elige variante y baja una Java 25 propia (Microsoft JDK 25.0.1). Releases antiguas sin sufijo cuentan como java21. NeoForge 21.1 con Java 25: probado solo a mano en la instancia de Endo.
- **2026-11-13: hotfix 1.0.1** (Alt+K no abria recompensas; ahora `OpenRewardsPayload` cliente->servidor, registrado junto a `RewardToastPayload`; causa exacta no confirmada, sin verificar en juego). Mini launcher en `launcher/` (release aparte `launcher-1.0.0`, ver `launcher/LEEME.md`).
- **Desde 2026-11-13: release 1.0.0** (`mod_version=1.0.0`, tag `v1.0.0`, rama `main`). Fin de la fase pre-release; mismo codigo que pre.5. Los releases de GitHub se hacen con `gh` (instalado en `C:Program FilesGitHub CLIgh.exe`) y llevan zips de perfiles Alto/Medio/Bajo y ServerFiles.
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

## Balance (2026-11-10)
- Catálogo de las 127 armas, munición y accesorios de la instancia de Endo: `docs/catalogo-tacz.md` (packs `tacz_default_gun` + `daffas`). `config/gitanoland/tacz_ids.txt` no existe en la instancia.
- **Implementado (sin verificar en juego):** los JSON `gunsmith.json` y `ammo_maker.json` ahora tienen `specialties` (id, weight, levels). Cada aldeano recibe una especialidad estable según su UUID (`TradeTables.specialty`): Gunsmith = pistolero(3), fusilero(3), escopetero(2, SMG+escopetas), francotirador(2), pesado(1); Ammo Maker = pistola(3), fusil(3), especial(2). Los `levels` sueltos del JSON serían comunes a todas. El título de la tienda muestra la especialidad (`gitanoland.specialty.<tienda>.<id>` en lang) y REI/JEI listan cada especialidad por separado. El nivel de cada trato se asigna por precio (<=100 nivel 1, <=300 nivel 2, <=600 nivel 3, <=1100 nivel 4, resto 5). Los JSON se generaron con un script a partir de los gun packs; todos los ids están validados.
- **Precios:** config del servidor `gunsmithPriceMultiplier` = 1.03 (+3 %) y `ammoMakerPriceMultiplier` = 1.0; se redondea al Kirky, así que lo muy barato no cambia. Armas "de broma" de Daffas (gerosa, southeastmomssecretweapon*, samula3) no se venden.
- **Al desplegar:** la copia de `config/gitanoland/trades/` de la instancia manda sobre la del jar; hay que borrarla para que cargue el JSON nuevo (y los aldeanos ya existentes cambian de tienda).
- TTK (no aplicado, en la instancia de Endo): en `config/tacz-server.toml` los multiplicadores base están a 1.0; en `config/tacztweaks.json` `bulletProtection` está en `false` (con `true`, Projectile Protection reduce balas; Protection normal no cuenta) y `balancing.playerDamage`/`playerHeadshot`/`armorIgnore` están a 1.0. Propuesta: `bulletProtection` true y `playerDamage` x0.7.
- Pendiente: forma de conseguir Kirkys (que los aldeanos compren cosas, recompensas por jefes, logros solo de arranque).
- Aviso: en `mods/` de la instancia hay dos jars de TaCZ (1.1.8-hotfix-r7 y 1.2.0).

## Tratos especiales de aldeanos normales (2026-11-11, sin verificar en juego)
- Sustituye a las misiones por Shift+clic. La GUI vanilla NO se toca: al clic derecho (`event/MissionEvents`, `mission/Missions.ensure`) se añaden a su lista, por cada nivel alcanzado, un **trato especial** (items difíciles -> Kirkys, datos en `data/gitanoland/missions.json`, 5 niveles por profesión; override en `config/gitanoland/missions.json`) y un **trueque normal con Kirkys** (2-5, copia aleatoria de uno de sus tratos: o compra el objeto pagando Kirkys, o lo vende cobrando Kirkys).
- Solo armorer, weaponsmith, toolsmith, cleric, librarian, cartographer; los de trueque fácil (fletcher, farmer, shepherd, butcher, fisherman, leatherworker, mason) no dan Kirkys.
- Especial: 2 usos (3 si la reputación vanilla con el jugador >= 20), nunca se reabastece (se re-bloquea al abrir; contador en datos del aldeano). Primer uso = sube al aldeano un nivel entero; 2.º y 3.º = la mitad de lo que falta. El de nivel 4 (penúltimo, el más difícil) lo sube a Maestro, da descuento (gossip MAJOR_POSITIVE +20) y reabastece los tratos normales. El de nivel 5 paga 50-100 Kirkys (fijo por aldeano según UUID). Casilla de resultado máx. 64: el exceso va directo al inventario (Endo dijo "bolsita temporal que se pierde"; lo interpreté así, confirmar).
- Quitadas las configs `missionMaxKirkys*` y `missionHints`. Datos en el aldeano: `gitanoland_lvl`, `gitanoland_special_idx`, `gitanoland_special_used`.
- `coinmod` era el nombre antiguo del mod (queda `config/coinmod-server.toml` huérfano en la instancia); todo vive en GitanoLand.

## Pendiente: perfiles de gráficos (Endo lo quiere para el final)
- Crear 3 instancias de Prism con menos gráficos cada una, a partir de la config de la instancia de Endo (shaders, physics mod, etc.). El perfil más bajo directamente NO lleva ese mod (para que el loader ni lo cargue). **No quitar el HUD** en ninguno. Se hace al final.
- Endo me deja hacer `git push` a la rama `alpha` sin preguntar cada vez (no a `main`, ni tags/releases).
- Texturas provisionales de zombi aldeano Gunsmith/Ammo Maker (`textures/entity/zombie_villager/profession/`): copia de las del aldeano (Endo las sustituirá).

## Tiendas v2 (2026-11-10, sin verificar en juego)
- **No se venden** (lista en el script generador, copiada aquí): lanzagranadas/RPG/minigun/M79 y munición explosiva (40mm, cohetes), armas de broma de Daffas (gerosa, southeast*, samula3, taurus*), y las "chetadas" `ak472nd`, `migraine3`, `mg338`, `sgputer2`, `ssg69s`; miras térmicas (`scope_reap_ir`); cargadores extendidos, mods de munición y `mgup*` ("upgrades"). Ninguna receta de arma de los packs usa end rods; la regla es: si la receta lleva netherite o nether star, el aldeano pide 1 (y un end rod si algún día la receta lo lleva).
- **Especialidades del Gunsmith:** pistolero(3), fusilero(3), escopetero(2: SMG+escopetas), cazador(2: DMR+francotiradores de caza), francotirador_pesado(1: daño>=40), ametrallador(1). Ammo Maker: pistola, fusil, especial.
- **Precio por TTK:** `dps = daño*rpm/60*(1+armor_ignore)` con fórmula y tope por clase (pistolas 50-450, SMG/escopetas 300-700, fusiles 350-700 (~500), caza 550-1100, francotiradores pesados 900-1300 (~1000), ametralladoras 900-1500). Materiales: <150 nada; <400 lingotes de hierro (4-24); >=400 1-3 diamantes (máx. 3) y lo raro de la receta. Los JSON se generan con un script (no está en el repo; la lógica está descrita aquí).
- **GUI de tienda:** panel 200x240, filas de 2 líneas (nombre arriba, precio y materiales abajo con iconos pequeños), título y saldo en líneas separadas.
- **Aldeanos:** al abrir la tienda se quedan quietos y te miran (`setTradingPlayer`, se libera al cerrar o si te alejas >8). Si llevas Kirkys en la mano, los aldeanos-tienda en 12 bloques se acercan y, al llegar, te ofrecen una oferta que puedas pagar (barra de acción, cooldown 1 min por aldeano): `event/ShopApproach`.

## Recompensas y ofertas en mano (2026-11-11, sin verificar en juego)
- Las ofertas de los aldeanos-tienda ya no salen en la barra de acción: el aldeano **sostiene el objeto en la mano** (se ve cruzado de brazos) mientras llevas Kirkys; lo guarda al abrir la tienda, al soltar los Kirkys o tras 1 min, y descansa 1 min (`event/ShopApproach`).
- **Logros:** normales 3 Kirkys, de desafío (morados) 7 (aviso con un toast propio, `net/RewardToastPayload` + `client/RewardToast`) (configs `achievementKirkys`, `achievementChallengeKirkys`); solo cuentan los que tienen display y padre (no recetas ni pestañas raíz). Quedan **pendientes** en el jugador (`gitanoland_pending`) y se reclaman en el menú (`reward/Rewards`, `RewardEvents`).
- **Menú de recompensas** (`reward/RewardsMenu`, `client/RewardsScreen`, panel gris oscuro con borde verde): botón de logros pendientes y de recompensa diaria (`dailyKirkys`=5, una vez por día real del servidor, sin rachas; **solo en servidores dedicados**: en un jugador o LAN no se muestra ni se puede reclamar). Se abre con **Alt+K** (`client/ClientKeys`, lleva modificador Alt para no chocar con teclas sueltas de otros mods, categoría propia) o `/gitanoland rewards`.
- Pendiente (Endo): el sistema de login diario con rachas para servidores irá en **otro .jar**, no aquí.

## Teclas sin conflictos (pre.3, 2026-11-12, sin verificar en juego)
- `client/KeyContexts` reasigna al arrancar el `KeyConflictContext` de teclas de otros mods (config cliente `fixKeyContexts`, true por defecto): `key.dragon_*` solo montado en dragon de Ice and Fire; `key.securitycraft.camera*` solo viendo una camara; teclas de TaCZ (nombre con "tacz") siguen activas como siempre pero ya no se marcan en conflicto con otros mods; las de otros mods que comparten tecla con una de esas pasan a un contexto "sin arma/dragon/camara" (R, Z, G, H, C... son del arma cuando la llevas).
- Solo toca teclas cuyo contexto era IN_GAME y que chocan con las anteriores; el resto (voice chat, mapa...) queda igual. El log escribe `[GitanoLand] Conflicto restante en ...` con lo que no se resuelve por contexto (sobre todo emotes de Punchy Moves, Quark, Iris, Cinematic `H`/Alt izq., Do a Barrel Roll): eso hay que cambiarlo a mano en Controles (Controlling filtra los conflictos).

## Teclas sin conflictos, ampliado (pre.4, 2026-11-13, sin verificar en juego)
- pre.3 solo tocaba teclas con contexto IN_GAME y en la instancia de Endo movio 1 sola (Iris recargar): Quark, mochila, voice chat, dragon y camara usan contextos propios. pre.4 **envuelve** el contexto original de cada tecla (`KeyContexts.Ctx`, conserva `isActive`/`conflicts` del original y solo anade exclusiones): una tecla ajena que comparte combinacion con una de TaCZ no vale con arma en la mano; con una de dragon (`key.dragon_*`), no vale montado en dragon; con una de camara de SecurityCraft, no vale viendo una camara. Las de TaCZ/dragon/camara quedan marcadas sin cambiar su comportamiento. Vanilla y contextos de pantalla no se tocan.
- El log escribe `Tecla X (contexto Y) excluida con [..]` y `Conflicto restante en ...`: revisar ahi que contextos usa cada mod.

## Teclas, pre.5 (2026-11-13, sin verificar en juego)
- pre.4 calculaba las exclusiones al arrancar y emparejaba teclas sin asignar (UNKNOWN) entre si: asignar el microfono en la sesion lo dejaba desactivado con arma. pre.5: todas las teclas de mods se envuelven (`Ctx` FREE) y `isActive()` mira EN CADA PULSACION si la combinacion actual choca con una tecla de TaCZ/dragon/camara (`clashes`); las sin asignar nunca chocan. Conflictos entre dos teclas "libres" ya no se ocultan (son reales).
