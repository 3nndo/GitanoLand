import json, os
os.chdir(r"D:\Claudesplan\GitanoLand")
RES = r"src\main\resources\assets\gitanoland"
BOOK = os.path.join(RES, "book", "guide")
NL = "<NEWLINE>"


def w(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w", encoding="utf-8", newline="\n").write(text)


def item(i, x, y, page=0, scale=2.0):
    return {"item": i, "x": x, "y": y, "page": page, "scale": scale}


def recipe(r, x, y, page=1, scale=1.3):
    return {"recipe": r, "shapeless": False, "x": x, "y": y, "page": page, "scale": scale}


# (id, titulo es, titulo en, icono, parrafos, items de la 2a hoja, recetas de la 2a hoja)
CH = [
    ("kirkys", "Kirkys", "Kirkys", "gitanoland:kirky",
     ["Los Kirkys son la moneda de GitanoLand. Se apilan de 64 y empiezas con 100.",
      "Cómo conseguirlos:",
      "• Recompensa diaria: 10 (se reclama en el menú de recompensas, tecla Alt + K).",
      "• Logros: 5 los normales y 12 los morados (de desafío).",
      "• Misiones de los aldeanos y trueques normales.",
      "• El Stoner de Smokeleaf.",
      "• Los juegos del casino (Jackpot)."],
     [("gitanoland:kirky", 40, 40, 3.0), ("minecraft:emerald", 120, 60, 2.0), ("minecraft:gold_ingot", 40, 120, 2.0), ("minecraft:diamond", 120, 140, 2.0)], []),
    ("bolsas", "Bolsas de Kirkys", "Kirky bags", "gitanoland:kirky_bag_large",
     ["Guardan solo Kirkys. Clic derecho: guarda los que lleves. Shift + clic derecho: saca un stack.",
      "Pequeña 500, normal 1000 y grande 3000 (pide nether star). Al mejorarla se conservan los Kirkys."],
     [], [("gitanoland:kirky_bag_small", 20, 20), ("gitanoland:kirky_bag", 20, 100), ("gitanoland:kirky_bag_large", 210, 60)]),
    ("tiendas", "Las tiendas", "The shops", "gitanoland:gunsmith_spawn_egg",
     ["Clic derecho a un aldeano con profesión de tienda:",
      "• Gunsmith: vende armas y accesorios.",
      "• Ammo Maker: vende munición.",
      "• WeedsGuy: cosas de Smokeleaf. Aparece solo de vez en cuando, como el mercader errante.",
      "Se paga con Kirkys y a veces con materiales. Si le pegas a un aldeano, suben los precios."],
     [("gitanoland:gunsmith_spawn_egg", 40, 40, 3.0), ("gitanoland:ammo_maker_spawn_egg", 120, 60, 3.0), ("gitanoland:weedsguy_spawn_egg", 40, 140, 3.0)], []),
    ("especialidades", "Especialidades", "Specialties", "minecraft:name_tag",
     ["Cada Gunsmith y Ammo Maker vende una especialidad: pistolas, fusiles, escopetas, rifles de caza...",
      "Botón CAMBIAR (arriba a la derecha de la tienda): pagas Kirkys y el aldeano cambia de especialidad. Cuanto más rara es la nueva, más cuesta.",
      "La especialidad elegida se queda guardada en ese aldeano."],
     [("minecraft:name_tag", 60, 60, 4.0), ("gitanoland:kirky", 150, 90, 3.0)], []),
    ("aldeanos", "Misiones de aldeanos", "Villager missions", "minecraft:emerald",
     ["Los aldeanos normales tienen un trato especial por nivel: piden objetos difíciles y pagan Kirkys (hasta 150).",
      "Cada trato se puede hacer 2 veces (3 si le caes muy bien).",
      "El Stoner de Smokeleaf está en todas las aldeas y paga casi todo en Kirkys."],
     [("minecraft:netherite_ingot", 50, 40, 2.5), ("minecraft:nether_star", 130, 40, 2.5), ("minecraft:echo_shard", 50, 130, 2.5), ("minecraft:enchanted_golden_apple", 130, 130, 2.5)], []),
    ("armas", "Las armas", "Weapons", "tacz:gun_smith_table",
     ["Las armas se craftean en la mesa de armas de TaCZ y son CARAS a propósito: mucho hierro y oro, y netherite o nether star las más pesadas.",
      "Las pistolas y las de recámara manual cuestan menos.",
      "También se pueden comprar al Gunsmith con Kirkys (ahí salen más baratas que antes)."],
     [("minecraft:iron_ingot", 50, 40, 3.0), ("minecraft:gold_ingot", 130, 40, 3.0), ("minecraft:netherite_ingot", 50, 130, 3.0), ("minecraft:nether_star", 130, 130, 3.0)], []),
    ("discos", "Discos", "Discs", "etched:blank_music_disc",
     ["Disco en blanco (Etched): 4 plástico de cáñamo (Smokeleaf) en cruz y 1 lingote de hierro.",
      "Copiar un disco: pon un disco de música y un disco en blanco en la mesa de crafteo. Sale una copia y el original se queda."],
     [("etched:blank_music_disc", 50, 60, 3.0), ("minecraft:music_disc_cat", 140, 60, 3.0)], [("gitanoland:blank_music_disc", 220, 40), ("gitanoland:copy_disc", 220, 130)]),
    ("casinos", "Casinos", "Casinos", "jackpot:red_slot_machine",
     ["Las tragaperras, el Plinko, las cartas y el billar (mod Jackpot) se configuran solas con Kirkys al colocarlas.",
      "El dueño de cada máquina puede cambiar precios y probabilidades en su pantalla de configuración.",
      "Y si un casino te parece una estafa... existe el ATENTADO."],
     [("jackpot:red_slot_machine", 50, 50, 3.0), ("jackpot:plinko", 140, 60, 3.0)], []),
    ("atentado", "Atentado en equipo", "Team attack", "minecraft:tnt",
     ["/atentado equipo [nombre] [jugador] [jugador]",
      "Sale ATENTADO para todos y tu equipo ve su vida en pantalla.",
      "/atentado chat: habla con tu equipo. /atentado fin: termina.",
      "/atentadohud mover: coloca el cuadro de vida."],
     [("minecraft:tnt", 60, 60, 4.0), ("minecraft:iron_sword", 150, 100, 3.0)], []),
    ("comandos", "Comandos", "Commands", "minecraft:command_block",
     ["/gitanoland rewards: menú de recompensas (también Alt + K).",
      "/gitanoland libro: te da este libro.",
      "/atentado ...: atentado en equipo.",
      "/atentadohud mover: cuadro de vida del equipo.",
      "/banco ...: banco digital (experimental, apagado por defecto)."],
     [("minecraft:command_block", 70, 70, 4.0)], []),
    ("servidor", "Servidor sin cuenta", "Cracked server", "minecraft:writable_book",
     ["Si el servidor no pide cuenta de Microsoft, la primera vez tienes que registrarte:",
      "/register contraseña contraseña",
      "Y las siguientes veces:",
      "/login contraseña",
      "Si juegas con una cuenta de Microsoft entras solo."],
     [("minecraft:writable_book", 70, 70, 4.0)], []),
]

for lang in ("es_es", "en_us"):
    intro = ["Bienvenido a GitanoLand.", NL, "Elige un tema:"]
    w(os.path.join(BOOK, lang, "root.txt"), "\n".join(intro) + "\n")
    for cid, tes, ten, icon, paras, items, recipes in CH:
        # Sin <NEWLINE> al principio (Citadel ya deja hueco para el icono) ni al final (se veia escrito).
        lines = []
        for p in paras:
            lines += [p, NL]
        w(os.path.join(BOOK, lang, cid + ".txt"), "\n".join(lines[:-1]) + "\n")

# Botones: 4 en la pagina izquierda (debajo del texto) y 7 en la derecha. El icono lo pone GuideBookScreen (icons.json).
LEFT_X, RIGHT_X = 97, 291
links, icons = [], {}
for i, ch in enumerate(CH):
    if i < 4:
        x, y = LEFT_X, 80 + i * 22
    else:
        x, y = RIGHT_X, 12 + (i - 4) * 22
    links.append({"linked_page": ch[0] + ".json", "text": "gitanoland.book." + ch[0], "x": x, "y": y, "page": 0, "icon": ch[3]})
root = {"parent": "", "text": "root.txt", "title": "gitanoland.book.title", "linked_page_buttons": links, "images": [],
        "item_renders": [], "recipes": [], "tabula_renders": [], "entity_renders": [], "entity_buttons": []}
w(os.path.join(BOOK, "root.json"), json.dumps(root, indent=2, ensure_ascii=False) + "\n")
if os.path.exists(os.path.join(BOOK, "icons.json")):
    os.remove(os.path.join(BOOK, "icons.json"))
for cid, tes, ten, icon, paras, items, recipes in CH:
    page = {"parent": "root.json", "text": cid + ".txt", "title": "gitanoland.book." + cid, "linked_page_buttons": [], "images": [],
            "item_renders": [item(icon, 25, 14, 0, 2.0)] + [item(i, x, y, 1, s) for (i, x, y, s) in items],
            "recipes": [recipe(r, x, y, 1, 1.3) for (r, x, y) in recipes], "tabula_renders": [], "entity_renders": [], "entity_buttons": []}
    w(os.path.join(BOOK, cid + ".json"), json.dumps(page, indent=2, ensure_ascii=False) + "\n")

for f, key in (("es_es.json", 1), ("en_us.json", 2)):
    pth = os.path.join(RES, "lang", f)
    d = json.load(open(pth, encoding="utf-8"))
    d["gitanoland.book.title"] = "Guía de GitanoLand"
    d["item.gitanoland.guide_book"] = d["gitanoland.book.title"]
    d["gitanoland.guide.title"] = d["gitanoland.book.title"]
    for cid, tes, ten, *_ in CH:
        d["gitanoland.book." + cid] = tes
    json.dump(d, open(pth, "w", encoding="utf-8", newline="\n"), indent=2, ensure_ascii=False)

json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/written_book"}},
          open(os.path.join(RES, "models", "item", "guide_book.json"), "w", encoding="utf-8"), indent=2)
print("assets ok:", len(CH), "capitulos")
