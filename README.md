# You Died — 1.12.2 Backport

A Minecraft mod that replaces the vanilla death screen with a Dark Souls inspired
**"YOU DIED"** splash and a death sting, fading into the normal respawn menu.

This is an **unofficial 1.12.2 Forge backport** of [Gory_Moon's You Died](https://www.curseforge.com/minecraft/mc-mods/you-died).
The upstream mod targets 1.16+ (Mojang mappings). There is no official 1.12.2 release,
so this port brings the effect to legacy Forge.

## Requirements

- Minecraft **1.12.2**
- Forge **14.23.5.2860+**
- Client-side only (`clientSideOnly` — safe to use on any server, no server install needed)

## What it does

On death, Forge's `GuiOpenEvent` swaps the vanilla `GuiGameOver` for an animated splash:

1. Black letterbox bands and the serif "YOU DIED" title fade in (~1s).
2. The title holds, then fades out (~4s in).
3. The screen hands off to the vanilla death menu (respawn / title buttons, cause of
   death, score), which fades in from black (~5.3s in).

No vanilla class is replaced or re-registered — the screen swap is purely additive via
the Forge event hook, so it stays compatible with other mods that touch the death screen.

## Porting notes (1.20 → 1.12.2)

| Upstream (1.20) | 1.12.2 |
|-----------------|--------|
| `DeathScreen` + `DeathScreenWrapper` | subclass `GuiGameOver` directly |
| `ScreenEvent.Opening` | `GuiOpenEvent` |
| `GuiGraphics` / `PoseStack` | direct `GlStateManager` + `Gui` draw calls |
| `Component.translatable` | `I18n` / vanilla title (handled by `GuiGameOver`) |
| `SimpleSoundInstance.forUI` | `PositionedSoundRecord.getMasterRecord` |
| custom **Times TTF** font (`font/times.json` provider) | baked PNG texture — 1.12.2 has no TTF/font-provider support |
| architectury multiloader (fabric/quilt/forge) | single Forge module, RetroFuturaGradle |

The "YOU DIED" text is a pre-baked texture (`textures/gui/you_died.png`) rendered from
the original Times font, since 1.12.2 cannot load TrueType fonts at runtime.

## Building

```
./gradlew build
```

Built with [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle),
JDK 8, MCP `stable_39`. Output jar lands in `build/libs/`.

## Credits

- **Gory_Moon** — original mod author
- Darkosto — original feature request
- 1.12.2 backport by **hunterhaunter**

## License

MIT (inherited from upstream).
