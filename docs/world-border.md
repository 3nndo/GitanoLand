# World border del servidor (15.000 de diametro)

Solo servidor. Mod: **DimWorldBorder 1.1.1** (`dimworldborder-1.21.1-1.1.1-universal.jar`, Modrinth `KNfZi3IU`, solo servidor, licencia All Rights Reserved: **no se sube a las releases publicas**, se copia a mano a la carpeta `mods` del servidor).

Tamanos pedidos (diametro): Overworld 15000, Nether 1875 (1/8), End 7500 (la mitad; se puede poner 15000).

Pasos (una vez, con el servidor encendido y siendo OP):
1. Overworld: `/worldborder set 15000` (vanilla) o con el mod estando en el Overworld.
2. Nether y End: el mod aplica el tamano a la dimension en la que estas (`/dwb ...`; los subcomandos, con el tabulador). Sin verificar la sintaxis exacta: la comprobe leyendo el jar, no en un servidor.
3. El mod guarda los ajustes en el mundo y en su `dimworldborder-server.toml` (se crea al primer arranque; formato no verificado).
4. Chunky (ya en el pack) solo para la zona central: 15000 de diametro son ~700.000 chunks.

Sin probar en servidor. Alternativa: BorderLine (CurseForge, aplica 0,125 al Nether y 0,5 al End solo).
