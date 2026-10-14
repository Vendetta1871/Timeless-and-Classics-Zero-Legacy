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
`--stdout` previews, `--force` overwrites.

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
| `FriendlyByteBuf` | `net.minecraft.network.PacketBuffer` (`new PacketBuffer(buf)` in `IMessage`) |
| `SimpleChannel` + `NetworkEvent.Context` | `SimpleNetworkWrapper` + `IMessage`/`IMessageHandler`, `MessageContext` |
| `DeferredRegister` / `RegistryObject` | `RegistryEvent.Register<T>` handler + static fields |
| `ForgeConfigSpec` | Forge `Configuration`, wrapped so callers keep `.get()` (see `legacy` config shim) |
| `ItemStack.EMPTY`, `isEmpty()` | same in 1.12.2 |
| `entity.level()` | `entity.world` |
| `player.getMainHandItem()` | `player.getHeldItemMainhand()` |

Packet handlers run on the Netty thread in 1.12.2: always wrap handling in
`FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(..)` (client side:
`Minecraft.getMinecraft().addScheduledTask(..)`).

When unsure about a 1.12.2 name, look it up in `build/rfg/minecraft-src` rather than guessing.

## Work split

The main session ports the hard parts by hand; routine parts are delegated. **Reserved for the main session
(do not edit unless asked):**

- `legacy/render/**` — PoseStack / VertexConsumer / RenderType shims over 1.12 `BufferBuilder` + `GlStateManager`
- `client/model/**`, `client/renderer/**`, `client/event/**`, `client/gameplay/**`, `client/sound/**`, `client/animation/**`
- `client/resource/**` except `client/resource/pojo/**` and `client/resource/serialize/**`
  (`client/resource/pojo/model/**` is reserved too: bedrock geometry POJOs)
- gun pack loading: `resource/*.java` (top level), `resource/manager/**`, `resource/network/**`
- `entity/EntityKineticBullet.java`, `mixin/**`, `util/RenderHelper.java`
