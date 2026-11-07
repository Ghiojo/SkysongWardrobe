# SkysongWardrobe

A Paper 1.21.11 plugin that lets players wear SkysongItems items in a wardrobe menu.
Each worn item can carry skin layers, which are drawn over the player's own skin.

## Requirements

- Paper 1.21.11 and Java 21.
- [SkysongItems](https://github.com/Ghiojo/Skysong-Special-Items): required.
  Every item tag the wardrobe reads lives there.
- SkysongStats: optional.
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
| `/sw reload` | Reloads every config file | `skysongwardrobe.admin` |

The gallery and apply permissions are lists in `config.yml`. Holding any one of them is
enough, so permissions from other plugins can be listed there. By default they are
`skysongwardrobe.gallery` and `skysongwardrobe.apply`.

Before a skin is downloaded, `/sw upload` and `/sw edit ... link` ask the player to agree
to the upload terms.

## Item tags

Wardrobe items are SkysongItems items with these tags:

| Tag | Value |
| --- | --- |
| `wardrobe_type` | Which wardrobe slots take the item, such as `coat`. Must match a slot's `accepts`. |
| `wardrobe_layer` | The regular layer, written by `/sw apply ... regular`. |
| `wardrobe_dyable_layer` | The dyed layer, written by `/sw apply ... dyed`. |
| `dye_color` | The tint for the dyed layer, as a packed RGB number. |

Layer tags hold `uuid:id`, so they survive name changes. Players see them as `name:id`.

## Configuration

Run `/sw reload` after editing any of these files.

- `config.yml`: permissions, download limits, the upload cooldown and the MineSkin API key.
  Turn on `verbose` to log silent failures, such as a worn item whose skin file is missing.
- `wardrobe.yml`: the wardrobe and gallery menus. Each wardrobe slot has a `position`
  (0 to 53), the `wardrobe_type` values it `accepts`, and a `priority`; higher priorities
  are drawn over lower ones. If the file has an error, the wardrobe stays closed and the
  error is logged until it is fixed.
- `messages.yml`: every message, in MiniMessage format.

## Data files

Inside `plugins/SkysongWardrobe/`:

- `skins/`: one folder per gallery owner, named after the player. The folder keeps the
  owner's UUID inside and is renamed when the player changes name.
- `deleted/`: a copy of every image removed with `/sw del`.
- `wardrobes/`: what each player is wearing.
- `texture-cache.yml`: looks MineSkin has already signed, so they are not uploaded again.
- `render-queue.yml`: looks still waiting for MineSkin.

## MineSkin setup

New looks only appear once MineSkin has signed them. Without an API key the wardrobe
still works, but new looks are not shown to players.

1. Create an account at [account.mineskin.org](https://account.mineskin.org).
2. Create an API key there.
3. Put it in `config.yml` under `mineskin.api-key`, then run `/sw reload`.

Keep the key private. If MineSkin asks the plugin to slow down, uploads pause and resume
by themselves. `render.upload-cooldown-seconds` sets the shortest time between two
uploads for the same player.

## Development

Tests use JUnit and MockBukkit: `./gradlew test`.
