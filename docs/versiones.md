# Versiones de GitanoLand

Cada commit hasta ahora es una versión **alpha 0.N** (tag `alpha-0.N`). Desde el 12-11-2026 empieza la fase **pre-release** (`1.0.0-pre.N`, tag `v1.0.0-pre.N`, rama `pre-release`).

| Versión | Tag | Commit | Fecha | Cambio |
|---|---|---|---|---|
| alpha 0.1 | `alpha-0.1` | `38e284c` | 10-11-2026 | initial commit |
| alpha 0.2 | `alpha-0.2` | `f553ccc` | 10-11-2026 | Añadir código fuente del mod GitanoLand y configuración de Gradle |
| alpha 0.3 | `alpha-0.3` | `2d55580` | 10-11-2026 | alpha-1.0: REI con crafteos de Smokeleaf y arreglo del efecto con F5 |
| alpha 0.4 | `alpha-0.4` | `b1a6c6e` | 10-11-2026 | Documentar ramas y releases |
| alpha 0.5 | `alpha-0.5` | `6f1e329` | 10-11-2026 | Corregir mod_version a 1.0.0-alpha (alpha-1.0 no es valida para NeoForge) |
| alpha 0.6 | `alpha-0.6` | `4ca9619` | 10-11-2026 | Descripciones y proveedores de efectos de Smokeleaf para jeed |
| alpha 0.7 | `alpha-0.7` | `ef7f746` | 10-11-2026 | Catalogo de armas TaCZ y notas de balance |
| alpha 0.8 | `alpha-0.8` | `843d17b` | 10-11-2026 | Especialidades por aldeano (Gunsmith y Ammo Maker), +3% en Gunsmith y tratos ampliados |
| alpha 0.9 | `alpha-0.9` | `7aeafd8` | 10-11-2026 | Misiones de aldeanos normales: Kirkys por objetos dificiles (max 20, 45 con Heroe del Pueblo) |
| alpha 0.10 | `alpha-0.10` | `528ce5a` | 10-11-2026 | Texturas provisionales de zombi aldeano (Gunsmith y Ammo Maker) |
| alpha 0.11 | `alpha-0.11` | `e3f9daf` | 10-11-2026 | Tiendas v2: sin explosivos ni upgrades, precios por TTK, materiales, nuevo GUI, aldeanos quietos y que se acercan con Kirkys |
| alpha 0.12 | `alpha-0.12` | `d0e4e69` | 11-11-2026 | Tratos especiales por nivel en la GUI vanilla de aldeanos normales, y trueques normales con Kirkys |
| alpha 0.13 | `alpha-0.13` | `46a9650` | 11-11-2026 | Ofertas en mano, Kirkys por logros, menu de recompensas (Alt+K) y recompensa diaria |
| alpha 0.14 | `alpha-0.14` | `ed0df3a` | 11-11-2026 | Recompensa diaria solo en servidores dedicados |
| alpha 0.15 | `alpha-0.15` | `0c3b67e` | 11-11-2026 | Toast de Kirkys por logro y recompensas a 3 y 7 |
| alpha 0.16 | `alpha-0.16` | `17bfe62` | 12-11-2026 | Texturas propias: aldeanos Gunsmith y Ammo Maker, bolsas de Kirkys y logo del mod |
| alpha 0.17 | `alpha-0.17` | `9d8671c` | 12-11-2026 | Texturas propias de los zombis aldeanos Gunsmith y Ammo Maker |
| **pre-release 1.0.0-pre.1** | `v1.0.0-pre.1` | (este commit) | 12-11-2026 | Inicio de la pre-release: mismo contenido que alpha 0.17 |
| **pre-release 1.0.0-pre.2** | `v1.0.0-pre.2` | (este commit) | 12-11-2026 | Texturas propias de `kirky` (151x149) y `tab_icon` (639x639) |
| **pre-release 1.0.0-pre.3** | `v1.0.0-pre.3` | (este commit) | 12-11-2026 | Contextos de teclas: las de TaCZ, dragon (Ice and Fire) y camara (SecurityCraft) ya no se pisan con las de otros mods |
| **pre-release 1.0.0-pre.4** | `v1.0.0-pre.4` | (este commit) | 13-11-2026 | Contextos de teclas ampliados: envuelven el contexto original de cada tecla (Quark, mochila, voice chat, dragon...) en vez de saltarse las que no eran "en juego" |
| **pre-release 1.0.0-pre.5** | `v1.0.0-pre.5` | (este commit) | 13-11-2026 | Teclas: la exclusion se calcula en cada pulsacion (no al arrancar) y ya no empareja teclas sin asignar |
| **release 1.0.0** | `v1.0.0` | (este commit) | 13-11-2026 | Primera version completa: mismo contenido que pre.5 (teclas sin conflictos entre mods) con perfiles Alto/Medio/Bajo y server files |
| **hotfix 1.0.1** | `v1.0.1` | (este commit) | 13-11-2026 | Hotfix: el menu de recompensas (Alt+K) se pide por un paquete propio en vez de por el comando de chat; se anade un log al pulsar la tecla |
| **release 1.0.2 (java21)** | `v1.0.2-java21` | rama `java21` | 13-11-2026 | Mismo codigo que 1.0.1. Pack estandar para Java 21 (Alto/Medio/Bajo + ServerFiles) |
| **release 1.0.2 (java25)** | `v1.0.2-java25` | rama `java25` | 13-11-2026 | Pack con Reimagined Intro 3.0.0 (solo carga con Java 25). Probado a mano con Java 25.0.1 de Microsoft |
| **release 1.0.3 (java21 / java25)** | `v1.0.3-java21`, `v1.0.3-java25` | ramas `java21`, `java25` | 14-11-2026 | Anade Pixel's AntiCheat 1.0-Beta (cliente y servidor, misma version) a perfiles y server pack; ServerFiles ya sin Punchy y con Waystones2Waypoints2 |
| **release 1.0.4 (java21 / java25)** | `v1.0.4-java21`, `v1.0.4-java25` | ramas `java21`, `java25` | 14-11-2026 | Blood moons (Enhanced Celestials 2 + CorgiLib + Data Anchor, cliente y servidor; datapack `gitanoland_bloodmoon` en `ServerFiles/datapacks`), Hostile Tactics (solo servidor), Euphoria Patches (Alto y Medio) |
| **release 1.0.5 (java21 / java25)** | `v1.0.5-java21`, `v1.0.5-java25` | ramas `java21`, `java25` | 14-11-2026 | Mundo mas lleno (solo servidor): When Dungeons Arise, Towns and Towers (+Cristel Lib), MOS Moog's Ocean Structures (+Moog's Structure Lib), Explorify. Brujulas Explorer's y Nature's (cliente y servidor). Cliente: Cubes Without Borders y Enhanced Celestials 2 Shader Support (Alto/Medio). Sin config de shaders todavia |
| **release 1.0.6 (java21 / java25)** | `v1.0.6-java21`, `v1.0.6-java25` | ramas `java21`, `java25` | 14-11-2026 | SkinRestorer (cliente y servidor, servidor crackeado). Config de shaders compartida (Complementary Unbound + Euphoria Patches, Alto/Medio). Simple Voice Chat con iconos visibles y activacion por voz. Tecla de Iris para apagar shaders sin asignar. Jar `gitanoland-1.0.6` |
