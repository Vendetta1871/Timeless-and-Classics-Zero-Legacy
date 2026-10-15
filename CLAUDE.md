# CLAUDE.md

Unofficial port of Timeless and Classics Zero (TACZ, Forge 1.20.1) to **Minecraft 1.12.2 Forge**.
Plan and scope: see `README.md`. The goal is to run original TACZ gun packs unchanged.

## Upstream reference

- In a linked git worktree `.upstream` is absent: use the main checkout's `/storage/Projects/tacz-legacy/.upstream`.
- `.upstream/TACZ` — TACZ branch `1.20.1`, commit `b482eff8c94a733ac8d0910193fca3893954027c` (git-ignored).
- `.upstream/SimpleBedrockModel` — branch `1.20.1-2.2.x`, the library TACZ bundles (only 3 classes used).
- The default gun pack lives in `.upstream/TACZ/src/main/resources/assets/tacz/custom/tacz_default_gun`.

## Build

Always go through `./gw` (wraps `gradlew`): Gradle itself must run on JDK 25 (RetroFuturaGradle 2), sources are
compiled by the JDK 17 toolchain with Jabel, and the client runs on JDK 8. `./gw` also overrides the
`org.gradle.java.home` pinned to JDK 8 in `~/.gradle/gradle.properties` and forwards `HTTPS_PROXY` to the JVMs.

```bash
./gw compileJava          # quick check
./gw build -x test
./gw runClient
```

RetroFuturaGradle (CleanroomMC TemplateDevEnv), MCP `stable_39` mappings, MixinBooter. JOML, commons-math3 and
LuaJ (FiguraMC fork) are shaded and relocated under `com.tacz.guns.shadow` (see `gradle/scripts/dependencies.gradle`);
source code imports the original packages. Decompiled Minecraft/Forge sources for lookups: `build/rfg/minecraft-src`.

## Port tool

`tools/port.py <path relative to com/tacz/guns> ...` copies upstream files into `src/` and rewrites imports and simple
type names that map 1:1 (table in the script). Always start a file with it, then fix the rest by hand.
`--stdout` previews, `--force` overwrites. It also converts simple `record`s to final classes, Java 9+ collection
factories to Guava, `Optional#ifPresentOrElse`, `"..".formatted(..)` and a list of common 1.20 entity/item/NBT calls;
extend its tables when you meet a mechanical rewrite more than twice.

`tools/restore_todos.py <Class> ...` uncomments the code under `// TODO(port): needs <Class>` markers once that class
exists; review the result and compile.

`build.gradle` has a temporary `portPending` list of sources excluded from compilation until their dependencies are
ported. Remove entries as they start compiling; it must end up empty.

## Porting rules

**Mirror upstream.** Every ported class keeps its upstream package, file name, class name, member names and
structure: `.upstream/TACZ/src/main/java/com/tacz/guns/X/Y.java` → `src/main/java/com/tacz/guns/X/Y.java`.
Keep upstream comments (Chinese comments stay as they are). Do not refactor or "improve" logic; only change what
the 1.12.2 API forces. This keeps future upstream diffs mergeable.

**Unported code.** Never leave a class half-ported silently. If a call cannot be ported yet, keep the line
commented out with `// TODO(port): <reason>` directly above it. Grep for `TODO(port)` to find all of them.

**New code** that has no upstream counterpart (shims, 1.12-only glue) goes into `com.tacz.guns.legacy.*`.

### Java

Jabel allows Java 17 *syntax*, but the runtime is **Java 8**:

- Allowed: `var`, switch expressions/arrow cases, pattern matching `instanceof`, diamond with anonymous classes.
- **Not allowed: `record`** (needs `java.lang.Record`). Convert to a `final class` with `private final` fields,
  a canonical constructor, record-style accessors (`foo()`, not `getFoo()`), `equals`/`hashCode`/`toString`.
- No Java 9+ library APIs: `List.of`/`Set.of`/`Map.of` (use `Collections.unmodifiableList(Arrays.asList(..))`,
  `ImmutableList.of` from Guava 21, etc.), `Stream.toList()` (use `collect(Collectors.toList())`),
  `Optional.isEmpty()`/`orElseThrow()` without args/`ifPresentOrElse`/`Optional.stream()`, `String.isBlank/strip/
  repeat/lines`, `InputStream.readAllBytes/transferTo`, `Path.of`, `Files.readString/writeString`,
  `Objects.requireNonNullElse`, `Map.entry`, `Collectors.toUnmodifiableList`, `Math.floorMod(long,int)` etc.
- Libraries shipped with 1.12.2 are old: **Gson 2.8.0** (no static `JsonParser.parseString/parseReader` → use
  `new JsonParser().parse(..)`; no `JsonObject.isEmpty()`, no `JsonArray.asList()`), **Guava 21**,
  commons-lang3 3.5, commons-io 2.5, Netty 4.1.9, log4j 2.8.1, **fastutil 7.1.0**.
- `javax.annotation.Nullable`/`Nonnull` (jsr305) are available, as are `org.jetbrains.annotations`.

### 1.20.1 → 1.12.2 API map (MCP stable_39 names)

| 1.20.1 | 1.12.2 |
|---|---|
| `ResourceLocation` (`net.minecraft.resources`) | `net.minecraft.util.ResourceLocation` |
| `ItemDisplayContext` | `ItemCameraTransforms.TransformType` (same constant names) |
| `Vec2` / `AABB` / `HitResult` | `Vec2f` / `AxisAlignedBB` / `RayTraceResult` |
| `Level` / `ServerLevel` / `ClientLevel` | `World` / `WorldServer` / `WorldClient` |
| `Entity` / `LivingEntity` / `Player` | `Entity` / `EntityLivingBase` / `EntityPlayer` |
| `ServerPlayer` / `LocalPlayer` | `EntityPlayerMP` / `EntityPlayerSP` |
| `CompoundTag` / `ListTag` / `Tag` | `NBTTagCompound` / `NBTTagList` / `NBTBase` |
| `stack.getTag()` / `hasTag()` / `getOrCreateTag()` | `getTagCompound()` / `hasTagCompound()` / set-if-null helper |
| `Component.translatable` / `literal` | `new TextComponentTranslation` / `new TextComponentString` |
| `ChatFormatting` | `TextFormatting` |
| `Vec3` / `Mth` / `Direction` / `InteractionHand` | `Vec3d` / `MathHelper` / `EnumFacing` / `EnumHand` |
| `FriendlyByteBuf` | `legacy.network.FriendlyByteBuf` (extends `PacketBuffer`, adds the 1.20 method names) |
| `SimpleChannel`, `NetworkEvent.Context`, `PacketDistributor`, `NetworkDirection` | same names in `legacy.network` (shim over an FML event channel; messages keep `encode/decode/handle`) |
| `DeferredRegister` / `RegistryObject` | `RegistryEvent.Register<T>` handler + static fields |
| `ForgeConfigSpec` | Forge `Configuration`, wrapped so callers keep `.get()` (see `legacy` config shim) |
| `ItemStack.EMPTY`, `isEmpty()` | same in 1.12.2 |
| `entity.level()` | `entity.world` |
| `player.getMainHandItem()` | `player.getHeldItemMainhand()` |

Packet handlers run on the Netty thread, as on 1.20.1: keep upstream's `context.enqueueWork(..)` (the shim schedules
it on the receiving side's main thread). `context.getNetworkManager().isMemoryConnection()` is `isLocalChannel()`.
`PacketDistributor.DIMENSION` takes the 1.12 dimension id (`entity.dimension`).

When unsure about a 1.12.2 name, look it up in `build/rfg/minecraft-src` rather than guessing.

## 1.12 shims (`com.tacz.guns.legacy`)

Ported code keeps upstream's 1.20 API shape through these shims; prefer them over rewriting call sites.

| 1.20.1 | shim |
|---|---|
| `PoseStack`, `VertexConsumer`, `RenderType`, `MultiBufferSource`, `RenderSystem`, `Axis`, BEWLR | `legacy.render` (CPU-transformed vertices on top of the GL matrix) |
| `ResourceManager`, `Resource`, `FileToIdConverter`, (Simple)`PreparableReloadListener`, `PackType` | `legacy.resource` (gun packs only; `GunPackRepository`) |
| `FriendlyByteBuf`, `SimpleChannel`, `NetworkEvent`, `PacketDistributor`, `NetworkDirection`, `NetworkRegistry` | `legacy.network` |
| `DeferredRegister`, `RegistryObject` (items, blocks, sounds, entities, potions, enchantments) | `legacy.registry` |
| `TagKey`, `BlockTags` (`state.is(tag)` → `tag.contains(state.getBlock())`, entity tags take the entity) | `legacy.tags` |
| `RecipeManager`, `RecipeType`; `level.getRecipeManager()` → `RecipeManager.of(level)` | `legacy.crafting` |
| `ForgeConfigSpec` | `legacy.config` |
| `GsonHelper`, `Util`, `ClipContext`, `FMLPaths`, `Mth` extras (`LegacyMath`), fastutil `Pair`, `Component.*` (`Components`), particle names (`LegacyParticles`), Java 9 `Optional`/`Objects` helpers (`Optionals`), item handler capability (`ItemHandlers`) | `legacy.util` |
| `Screen`, `AbstractContainerScreen`, `Button`, `ImageButton`, `EditBox`, `ForgeSlider`, `Renderable`, `GuiEventListener` (widgets are not in `buttonList`; the screen dispatches input to them) | `legacy.util.gui` |
| `IGuiOverlay` (drawn from `client.init.ClientSetupEvent#onRenderGameOverlay`), `ClientTooltipComponent` + `TooltipComponentHandler` (tooltip images over blank tooltip lines), `MutableComponent`, `FontUtil` (`Font#width/split`), `KeyInputs` (`InputEvent.Key/MouseButton` payload: `KeyInputs.matches/isPress`), `MinecraftAccess` (`startUseItem`) | `legacy.util` |

Gson 2.8 cannot build `EnumMap` fields: TACZ's Gson instances register `EnumMapTypeAdapterFactory`.

## Work split

The main session ports the hard parts by hand; routine parts are delegated. **Reserved for the main session
(do not edit unless asked):**

- `legacy/**` except `legacy/util/**` (new small helpers may be added there)
- `client/model/**`, `client/renderer/**`, `client/event/**`, `client/gameplay/**`, `client/sound/**`, `client/animation/**`
- `client/resource/**` except `client/resource/pojo/**` and `client/resource/serialize/**`
  (`client/resource/pojo/model/**` is reserved too: bedrock geometry POJOs)
- gun pack loading: `resource/*.java` (top level), `resource/manager/**`, `resource/network/**`
- `entity/EntityKineticBullet.java`, `mixin/**`, `util/RenderHelper.java`, `util/RenderDistance.java`, `GunMod.java`
