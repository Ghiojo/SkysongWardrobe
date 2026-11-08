# SkysongWardrobe

A Paper 1.21.11 plugin that lets players wear SkysongItems items in a wardrobe menu.
Each worn item can carry skin layers, which are drawn over the player's own skin.

## Requirements

- Paper 1.21.11 and Java 21.
- [SkysongItems](https://github.com/Ghiojo/Skysong-Special-Items): required.
  Every item tag the wardrobe reads lives there.
- SkysongStats: optional. When it is installed, items worn in the wardrobe count towards
  stats with `item-tags:` exactly like items in the inventory.
- WorldGuard 7: optional. When it is installed, region flags decide who may place, use and
  break mannequins (see [Mannequins](#mannequins)).
- Citizens 2.0.43 or newer for 1.21.11: optional. When it is installed, player NPCs get a
  wardrobe too (see [Citizens NPCs](#citizens-npcs)).
- A MineSkin API key, so new looks can be shown to players (see [MineSkin setup](#mineskin-setup)).

## Building

```
./gradlew build
```

The plugin jar is written to `build/libs/`. It already contains the MineSkin client, so
drop it into `plugins/` next to SkysongItems and restart the server.

## How it works

1. A player uploads a 64x64 skin image to their **gallery** with `/sw upload`.
   Each gallery entry has an id, a name and an icon.
2. A staff member applies a gallery entry to a SkysongItems item with `/sw apply`.
   The entry is stored on the item as a layer: a regular one, or a dyed one that is tinted
   by the item's dye colour.
3. A player puts items in the slots of their **wardrobe** (`/wardrobe`).
4. When the wardrobe closes, the layers of every worn item are drawn over the player's
   own skin, in slot priority order. The plugin uploads the result to MineSkin to get it
   signed, then shows it to everyone. Looks that were signed before are reused at once.

The player's arm model (classic or slim) is kept from their own skin. MineSkin uploads go
through a queue that is saved to disk, so a pending look survives a restart, and each
player only ever has one look waiting.

## Commands

`/sw` is short for `/skysongwardrobe`.

| Command | What it does | Permission |
| --- | --- | --- |
| `/wardrobe [player]` | Opens your wardrobe, or another player's | `skysongwardrobe.wardrobe` (`.other` for another player) |
| `/sw gallery [player]` | Opens your gallery, or another player's | a gallery permission (`.other` for another player) |
| `/sw upload <id> <link> [icon] [name...]` | Adds a skin from a link to your gallery | a gallery permission |
| `/sw edit <id> <name\|icon\|link> <value...>` | Changes an entry of your gallery | a gallery permission |
| `/sw del <id>` | Removes an entry from your gallery | a gallery permission |
| `/sw apply [player:]id <regular\|dyed>` | Puts a gallery entry on the item in your main hand | an apply permission (`.other` for another player's entry) |
| `/sw unapply <regular\|dyed>` | Removes a layer from the item in your main hand | an apply permission |
| `/sw npc [number]` | Opens a Citizens NPC's wardrobe | an NPC permission (`.other` for NPCs you don't own) |
| `/sw npc removed` | Takes items back from removed NPCs' wardrobes | the `.other` variant of an NPC permission |
| `/sw reload` | Reloads every config file | `skysongwardrobe.admin` |

The gallery and apply permissions are lists in `config.yml`. Holding any one of them is
enough, so permissions from other plugins can be listed there. By default they are
`skysongwardrobe.gallery` and `skysongwardrobe.apply`.

Before a skin is downloaded, `/sw upload` and `/sw edit ... link` ask the player to agree
to the upload terms. Links to the server itself or its private network are refused,
including through redirects.

## Item tags

Wardrobe items are SkysongItems items with these tags:

| Tag | Value |
| --- | --- |
| `wardrobe_type` | Which wardrobe slots take the item, such as `coat`. Must match a slot's `accepts`. |
| `wardrobe_layer` | The regular layer, written by `/sw apply ... regular`. |
| `wardrobe_dyable_layer` | The dyed layer, written by `/sw apply ... dyed`. |
| `dye_color` | The tint for the dyed layer, as a packed RGB number. |

Layer tags hold `uuid:id`, so they survive name changes. Players see them as `name:id`.

Mannequin spawners use two more tags, described under [Mannequins](#mannequins).

## Mannequins

A mannequin is a player-shaped stand with its own wardrobe. It shows its outfit just like
a player does, and it never moves, turns its head or shows a name. Its arms still sway a
little, because the game itself animates them.

### Spawner items

Any SkysongItems item with a `mannequin_skin` tag is a mannequin spawner. These items can
be crafted, bought and sold like any other item.

| Tag | Value |
| --- | --- |
| `mannequin_skin` | The base skin the mannequin wears under its outfit: the name of a file in `mannequin-skins/`, without `.png`. |
| `mannequin_model` | `slim` for slim arms; anything else, or no tag, means classic arms. |

To place a mannequin, right-click the top of a block with a spawner. The mannequin needs
two free blocks above that block, and it faces the player, rounded to the nearest 45°.
The spawner is used up and saved with the mannequin, so the same item comes back when the
mannequin is picked up.

### Using a mannequin

- **Right-click** opens its wardrobe. Only one player can dress a mannequin at a time, and
  its new look is drawn when the menu closes. Player looks always go before mannequin looks
  in the MineSkin queue.
- **Sneak and punch** picks it up. The spawner and everything the mannequin wore drop on
  the ground. Punching without sneaking only shows a hint, and mannequins can't be hurt.

The mannequin wardrobe has the same slots as a player's, plus six **equipment** slots in
the last two columns of the menu. Items there are worn as real equipment, so armour and
held items show on the mannequin:

| Equipment | Menu position |
| --- | --- |
| Head | 7 |
| Chest | 16 |
| Legs | 25 |
| Feet | 34 |
| Main hand | 17 |
| Off hand | 26 |

These positions are built in. The last two columns (positions 7, 8, 16, 17, 25, 26, 34,
35, 43, 44, 52 and 53) are kept for them, so `wardrobe.yml` can't put slots there, and
slot ids can't start with `equipment:`. Items worn by mannequins never count towards
SkysongStats.

### Base skins

Put the base skins in `plugins/SkysongWardrobe/mannequin-skins/` as 64x64 PNG files.
The file name is the skin's id, so `stone.png` is the skin `stone`. Names may only use
lowercase `a-z`, `0-9` and `_`; other files are skipped with a warning. Run `/sw reload`
after adding a skin. A spawner whose skin is missing can't place a mannequin.

### Who may do what

Placing a mannequin needs one of the permissions under `mannequins.permissions` in
`config.yml` (by default `skysongwardrobe.mannequin`). The `.other` variant of any of them,
such as `skysongwardrobe.mannequin.other`, makes a player staff: staff may open and pick up
every mannequin, anywhere.

Without WorldGuard, or outside every WorldGuard region, only a mannequin's owner and staff
may open it or pick it up.

With WorldGuard, inside a region the region decides, even for the owner. This plugin adds
three flags:

| Flag | Allows |
| --- | --- |
| `mannequin-place` | Placing mannequins |
| `mannequin-use` | Opening a mannequin's wardrobe |
| `mannequin-break` | Picking mannequins up |

Region members may do all three unless the flag is set to `deny`. Other players may only
where the flag is set to `allow`, for example `/rg flag shop mannequin-use allow`. Players with WorldGuard's region
bypass may always.

### Limits

`config.yml` caps how many mannequins there can be. `0` means no limit.

- `mannequins.max-per-player` (default `0`): how many mannequins one player may have placed.
  Staff are not limited by it.
- `mannequins.max-per-chunk` (default `15`): how many mannequins one chunk may hold, for
  everyone.

## Citizens NPCs

With Citizens installed, player-type NPCs (`/npc type player`, the default) can wear a
wardrobe just like players and mannequins. Other NPC types, such as zombies or mannequin
NPCs, can't.

### Opening an NPC's wardrobe

`/sw npc` opens the wardrobe of the NPC you selected with `/npc sel`, or else the one you
are looking at (within 5 blocks). `/sw npc <number>` opens one by its Citizens number, as
shown by `/npc list`. Only one person can dress an NPC at a time. Its new look is drawn
when the menu closes. In the MineSkin queue, player looks go first, then NPCs, then mannequins.

The NPC wardrobe has the same slots as a player's and no equipment slots; give NPCs
equipment with Citizens' own `/npc equip`. Items an NPC wears never count towards
SkysongStats.

### Base skin

An NPC's outfit is drawn over its current Citizens skin, so set one with `/npc skin` first.
The plugin remembers that skin before dressing the NPC, because once dressed the NPC only
holds the rendered look. Changing the skin with `/npc skin` later makes the new skin the
base the next time the wardrobe closes. Whether the NPC has slim or classic arms comes
from its base skin.

### Who may dress an NPC

Opening an NPC's wardrobe needs one of the permissions under `npcs.permissions` in
`config.yml` (by default `skysongwardrobe.npc`), and either:

- being the NPC's Citizens owner (`/npc owner <player>`), or
- holding the `.other` variant of one of those permissions, such as
  `skysongwardrobe.npc.other`, which reaches every NPC.

The access check is a list of rules where any one rule can allow, so later features (such
as NPCs that belong to a SkysongEconomy business) can add a rule without changing the others.

### Removed and copied NPCs

When an NPC is removed with `/npc remove`, anything in its wardrobe is moved to
`npcs/removed/` so nothing is lost. Staff can get those items back with
`/sw npc removed`: it lists the removed NPCs, newest first, and clicking one opens its
wardrobe, which items can only be taken out of. An entry is deleted once it's empty.

`/npc copy` gives the copy the same wardrobe and base skin as the original.

### Building with Citizens

The plugin compiles against Citizens from `maven.citizensnpcs.co`. If your internet
provider blocks that host, build once over a VPN; Gradle keeps the files afterwards.
Citizens is not bundled in the jar.

## Configuration

Run `/sw reload` after editing any of these files.

- `config.yml`: permissions, download limits, the upload cooldown, mannequin limits, NPC
  permissions, the MineSkin API key and skin visibility.
  Turn on `verbose` to log silent failures, such as a worn item whose skin file is missing.
- `wardrobe.yml`: the wardrobe and gallery menus. Each wardrobe slot has a `position`
  (0 to 53), the `wardrobe_type` values it `accepts`, and a `priority`; higher priorities
  are drawn over lower ones. If the file has an error, the wardrobe stays closed and the
  error is logged until it is fixed.
- `messages.yml`: every message, in MiniMessage format. The `hotbar` list names the
  sections or keys shown above the hotbar instead of in chat (by default the wardrobe,
  render, mannequin, npc, apply and unapply messages). A messages.yml without the list uses
  the default one.

## Data files

Inside `plugins/SkysongWardrobe/`:

- `skins/`: one folder per gallery owner, named after the player. The folder keeps the
  owner's UUID inside and is renamed when the player changes name.
- `deleted/`: a copy of every image removed with `/sw del`.
- `wardrobes/`: what each player is wearing.
- `texture-cache.yml`: looks MineSkin has already signed, so they are not uploaded again.
- `render-queue.yml`: looks still waiting for MineSkin.
- `mannequin-skins/`: the base skins mannequins can wear.
- `mannequins/`: one file per placed mannequin, with its owner, skin, arm model and saved
  spawner. `mannequins/wardrobes/` holds what each mannequin wears.
- `npcs/`: one file per dressed Citizens NPC, named by its UUID, with its base skin.
  `npcs/wardrobes/` holds what each NPC wears, and `npcs/removed/` keeps the wardrobes of
  removed NPCs until staff empty them.

## MineSkin setup

New looks only appear once MineSkin has signed them. Without an API key the wardrobe
still works, but new looks are not shown to players.

1. Create an account at [account.mineskin.org](https://account.mineskin.org).
2. Create an API key there.
3. Put it in `config.yml` under `mineskin.api-key`, then run `/sw reload`.

Skins are uploaded to MineSkin as **unlisted** by default, so they never show up in
MineSkin's public gallery or search. Set `mineskin.visibility: private` to also hide them
from anyone who has the link, if your MineSkin plan allows private skins; otherwise uploads
fail with "you are not allowed to generate private skins". Skins are never uploaded as public.

Keep the key private. If MineSkin asks the plugin to slow down, uploads pause and resume
by themselves. `render.upload-cooldown-seconds` sets the shortest time between two
uploads for the same player.

## Development

Tests use JUnit and MockBukkit: `./gradlew test`.
