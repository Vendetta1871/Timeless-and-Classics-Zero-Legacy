# TaCZ Legacy

An unofficial port of [Timeless and Classics Zero](https://github.com/MCModderAnchor/TACZ) (Forge 1.20.1) to
**Minecraft 1.12.2 Forge**.

The goal is to run original TACZ gun packs without conversion: the same `gunpack` format, the same Bedrock models,
animations and Lua scripts.

> Status: stage 1 is nearly done. Guns from the original gun packs work in single player and on a dedicated server:
> first-person models and animations, shooting, damage, reloading, scopes, sounds, HUD, gunsmith table.
> Tested with the default TACZ pack and with OptiFine (without shaders).

## Plan

### Stage 1 — the guns themselves

- Loading gun packs in the TACZ format (the `tacz/` folder and zip archives), including the default pack
- Items: guns, ammo, attachments, ammo boxes; gun NBT data
- Server-side shooting: fire modes, bullets with hitbox detection, damage, knockback, penetration
- Reloading, magazine inspection, fire mode switching, aiming, sprinting, melee
- First-person rendering: Bedrock models of guns and attachments, arms, animations (Lua state machines), muzzle
  flashes, shell casings
- Scopes and ADS, recoil and camera animations, FOV
- Sounds from gun packs
- Gunsmith table (crafting), HUD (ammo, crosshair, hit markers), tooltips
- Config, commands, networking
- Working alongside OptiFine **without shaders** (no crashes or rendering glitches)

### Stage 2 — third-person animations, shader compatibility, etc.

- Third-person poses (holding/aiming)
- OptiFine shader compatibility: stencil-based scopes, the shadow pass (muzzle flashes, shell casings)
- JEI and other integrations that exist for 1.12.2

### Not ported

Compatibility with mods that do not exist on 1.12.2: KubeJS, Oculus/Iris, Embeddium, Create, Carry On,
Shoulder Surfing Reloaded, Controllable, Player Animator, Accelerated Rendering.

## Differences from the original

What cannot be done on 1.12.2 the same way as on 1.20.1:

- Gunsmith table recipes from gun packs are read as is, but 1.20 tags are turned into OreDictionary entries
  (`forge:ingots/iron` → `ingotIron`). Materials that do not exist in 1.12.2 (copper, amethyst, netherite) are
  unavailable without other mods, so such recipes cannot be crafted.
- Crawling (the swimming pose) does not exist in 1.12.2: the state is synced, but the pose and hitbox do not change.
- Sounds "relative to the listener" are played at the player's position.

Done differently from the original:

- Optical scopes magnify only the image inside the lens (picture-in-picture: the part of the world the lens shows is
  rendered by a second, small pass with a magnified FOV), not the whole screen. The lens shows the magnified image with the reticle from the hip too,
  instead of a black circle, with a slight fisheye effect towards the lens edge. Settings are in
  `config/tacz-client.cfg` (the `render` section): `ScopePictureInPicture` (turning it off restores the original
  behaviour), `ScopeViewResolution` (resolution of the image in the lens), `ScopeFisheye` (distortion strength,
  0 turns it off). Under OptiFine shaders scopes work as in the original.

## Building

Gradle (RetroFuturaGradle 2) runs on JDK 25, sources are compiled by JDK 17 through Jabel into Java 8 bytecode, and
the client runs on JDK 8. The `./gw` script sets up the right JDKs; the path to JDK 25 is set by the `GRADLE_JDK`
variable (default `/storage/opt/jdk-25`).

TACZ assets are not stored in the repository: before the first build, fetch the pinned version of the original.

```bash
tools/fetch-upstream.sh
```

```bash
./gw build
```

```bash
./gw runClient
```

Testing with OptiFine: OptiFine only works in an obfuscated environment, so its jar goes into
`run/obfuscated/extra-mods/` and the client is started with `./gw runObfClient`.

The autotest (`TACZ_AUTOTEST=1`; it also works with `runObfClient`, and `TACZ_AUTOTEST_SERVER=host:port` connects to
a server) creates a flat world, gives an AK47 (`TACZ_AUTOTEST_GUN`), aims, shoots at a zombie, reloads and saves
screenshots to `run/screenshots` (`run/obfuscated/screenshots`). `TACZ_AUTOTEST_SCOPE=<attachment>` mounts a scope,
`TACZ_AUTOTEST_WORLD=default` uses generated terrain instead of the flat world.
`TACZ_AUTOTEST_WARMUP=<ticks>` waits first and logs the FPS.

The built jar is in `build/libs/`. Runtime dependency: [MixinBooter](https://github.com/CleanroomMC/MixinBooter).

## License

- Code — [GNU GPL 3.0](LICENSE), like the original TACZ.
- TACZ assets (including the default gun pack) — [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/),
  by the TACZ team. Redistributed unmodified only.
- Build scripts are based on [CleanroomMC TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) (MIT).
