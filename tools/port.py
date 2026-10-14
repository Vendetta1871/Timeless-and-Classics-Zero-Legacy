#!/usr/bin/env python3
"""Mechanical first pass of porting an upstream TACZ (1.20.1) source file to 1.12.2.

Copies .upstream/TACZ/src/main/java/com/tacz/guns/<path> to src/main/java/com/tacz/guns/<path> and rewrites
imports and simple type names that have a direct 1.12.2 equivalent (see TYPES / CALLS). Everything else is left
for hand-porting; run `./gw compileJava` afterwards and fix what remains.

Usage: tools/port.py [--force] [--stdout] <path relative to com/tacz/guns> ...
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def _upstream_root():
    # .upstream is git-ignored, so a linked worktree has none: fall back to the main checkout
    if (ROOT / '.upstream').is_dir():
        return ROOT / '.upstream'
    common = subprocess.run(['git', 'rev-parse', '--path-format=absolute', '--git-common-dir'],
                            cwd=ROOT, capture_output=True, text=True).stdout.strip()
    return Path(common).parent / '.upstream'


UPSTREAM = _upstream_root() / 'TACZ/src/main/java/com/tacz/guns'
TARGET = ROOT / 'src/main/java/com/tacz/guns'

# upstream FQN -> (1.12.2 FQN, new simple name or None when unchanged)
TYPES = {
    # render shims (com.tacz.guns.legacy.render)
    'com.mojang.blaze3d.vertex.PoseStack': 'com.tacz.guns.legacy.render.PoseStack',
    'com.mojang.blaze3d.vertex.VertexConsumer': 'com.tacz.guns.legacy.render.VertexConsumer',
    'com.mojang.math.Axis': 'com.tacz.guns.legacy.render.Axis',
    'net.minecraft.client.renderer.MultiBufferSource': 'com.tacz.guns.legacy.render.MultiBufferSource',
    'net.minecraft.client.renderer.RenderType': 'com.tacz.guns.legacy.render.RenderType',
    'net.minecraft.client.renderer.LightTexture': 'com.tacz.guns.legacy.render.LightTexture',
    'net.minecraft.client.renderer.texture.OverlayTexture': 'com.tacz.guns.legacy.render.OverlayTexture',
    'com.mojang.blaze3d.systems.RenderSystem': 'com.tacz.guns.legacy.render.RenderSystem',
    'net.minecraft.client.renderer.texture.MissingTextureAtlasSprite': ('com.tacz.guns.legacy.render.MissingTexture', 'MissingTexture'),
    'com.mojang.blaze3d.platform.GlStateManager': 'net.minecraft.client.renderer.GlStateManager',
    'net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer': 'com.tacz.guns.legacy.render.BlockEntityWithoutLevelRenderer',
    'net.minecraft.client.resources.language.I18n': 'net.minecraft.client.resources.I18n',
    # SimpleBedrockModel first-person API (com.tacz.guns.legacy.fp)
    'com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPGeoItemRenderer': 'com.tacz.guns.legacy.fp.IFPGeoItemRenderer',
    'com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance': 'com.tacz.guns.legacy.fp.IFPAnimationInstance',
    'com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler': 'com.tacz.guns.legacy.fp.FirstPersonRenderHandler',
    # resource shims (com.tacz.guns.legacy.resource)
    'net.minecraft.server.packs.resources.ResourceManager': 'com.tacz.guns.legacy.resource.ResourceManager',
    'net.minecraft.server.packs.resources.Resource': 'com.tacz.guns.legacy.resource.Resource',
    'net.minecraft.server.packs.resources.SimplePreparableReloadListener': 'com.tacz.guns.legacy.resource.SimplePreparableReloadListener',
    'net.minecraft.server.packs.resources.PreparableReloadListener': 'com.tacz.guns.legacy.resource.PreparableReloadListener',
    'net.minecraft.resources.FileToIdConverter': 'com.tacz.guns.legacy.resource.FileToIdConverter',
    'net.minecraft.util.profiling.ProfilerFiller': 'com.tacz.guns.legacy.resource.ProfilerFiller',
    'net.minecraft.server.packs.PackType': 'com.tacz.guns.legacy.resource.PackType',
    # common types
    'net.minecraft.resources.ResourceLocation': 'net.minecraft.util.ResourceLocation',
    'net.minecraft.world.item.ItemDisplayContext': ('net.minecraft.client.renderer.block.model.ItemCameraTransforms', 'ItemCameraTransforms.TransformType'),
    'net.minecraft.world.phys.Vec3': ('net.minecraft.util.math.Vec3d', 'Vec3d'),
    'net.minecraft.world.phys.Vec2': ('net.minecraft.util.math.Vec2f', 'Vec2f'),
    'net.minecraft.world.phys.AABB': ('net.minecraft.util.math.AxisAlignedBB', 'AxisAlignedBB'),
    'net.minecraft.world.phys.HitResult': ('net.minecraft.util.math.RayTraceResult', 'RayTraceResult'),
    'net.minecraft.world.phys.BlockHitResult': ('net.minecraft.util.math.RayTraceResult', 'RayTraceResult'),
    'net.minecraft.world.phys.EntityHitResult': ('net.minecraft.util.math.RayTraceResult', 'RayTraceResult'),
    'net.minecraft.core.BlockPos': 'net.minecraft.util.math.BlockPos',
    'net.minecraft.core.Direction': ('net.minecraft.util.EnumFacing', 'EnumFacing'),
    'net.minecraft.util.Mth': ('net.minecraft.util.math.MathHelper', 'MathHelper'),
    'net.minecraft.util.RandomSource': ('java.util.Random', 'Random'),
    'net.minecraft.world.level.Level': ('net.minecraft.world.World', 'World'),
    'net.minecraft.server.level.ServerLevel': ('net.minecraft.world.WorldServer', 'WorldServer'),
    'net.minecraft.client.multiplayer.ClientLevel': ('net.minecraft.client.multiplayer.WorldClient', 'WorldClient'),
    'net.minecraft.world.entity.Entity': 'net.minecraft.entity.Entity',
    'net.minecraft.world.entity.LivingEntity': ('net.minecraft.entity.EntityLivingBase', 'EntityLivingBase'),
    'net.minecraft.world.entity.player.Player': ('net.minecraft.entity.player.EntityPlayer', 'EntityPlayer'),
    'net.minecraft.server.level.ServerPlayer': ('net.minecraft.entity.player.EntityPlayerMP', 'EntityPlayerMP'),
    'net.minecraft.client.player.LocalPlayer': ('net.minecraft.client.entity.EntityPlayerSP', 'EntityPlayerSP'),
    'net.minecraft.client.player.AbstractClientPlayer': 'net.minecraft.client.entity.AbstractClientPlayer',
    'net.minecraft.world.item.ItemStack': 'net.minecraft.item.ItemStack',
    'net.minecraft.world.item.Item': 'net.minecraft.item.Item',
    'net.minecraft.world.item.Items': 'net.minecraft.init.Items',
    'net.minecraft.world.level.block.Block': 'net.minecraft.block.Block',
    'net.minecraft.world.level.block.state.BlockState': ('net.minecraft.block.state.IBlockState', 'IBlockState'),
    'net.minecraft.world.level.block.entity.BlockEntity': ('net.minecraft.tileentity.TileEntity', 'TileEntity'),
    'net.minecraft.nbt.CompoundTag': ('net.minecraft.nbt.NBTTagCompound', 'NBTTagCompound'),
    'net.minecraft.nbt.ListTag': ('net.minecraft.nbt.NBTTagList', 'NBTTagList'),
    'net.minecraft.nbt.Tag': ('net.minecraft.nbt.NBTBase', 'NBTBase'),
    'net.minecraft.nbt.StringTag': ('net.minecraft.nbt.NBTTagString', 'NBTTagString'),
    'net.minecraft.nbt.IntTag': ('net.minecraft.nbt.NBTTagInt', 'NBTTagInt'),
    'net.minecraft.nbt.FloatTag': ('net.minecraft.nbt.NBTTagFloat', 'NBTTagFloat'),
    'net.minecraft.network.FriendlyByteBuf': ('net.minecraft.network.PacketBuffer', 'PacketBuffer'),
    'net.minecraft.network.chat.Component': ('net.minecraft.util.text.ITextComponent', 'ITextComponent'),
    'net.minecraft.network.chat.MutableComponent': ('net.minecraft.util.text.ITextComponent', 'ITextComponent'),
    'net.minecraft.ChatFormatting': ('net.minecraft.util.text.TextFormatting', 'TextFormatting'),
    'net.minecraft.world.InteractionHand': ('net.minecraft.util.EnumHand', 'EnumHand'),
    'net.minecraft.world.entity.HumanoidArm': ('net.minecraft.util.EnumHandSide', 'EnumHandSide'),
    'net.minecraft.world.entity.EquipmentSlot': ('net.minecraft.inventory.EntityEquipmentSlot', 'EntityEquipmentSlot'),
    'net.minecraft.sounds.SoundEvent': 'net.minecraft.util.SoundEvent',
    'net.minecraft.sounds.SoundSource': ('net.minecraft.util.SoundCategory', 'SoundCategory'),
    'net.minecraft.world.damagesource.DamageSource': 'net.minecraft.util.DamageSource',
    'net.minecraft.client.gui.Font': ('net.minecraft.client.gui.FontRenderer', 'FontRenderer'),
    'net.minecraft.client.gui.screens.Screen': ('net.minecraft.client.gui.GuiScreen', 'GuiScreen'),
    'net.minecraft.client.KeyMapping': ('net.minecraft.client.settings.KeyBinding', 'KeyBinding'),
    'net.minecraftforge.client.event.ViewportEvent': 'net.minecraftforge.client.event.EntityViewRenderEvent',
    'net.minecraftforge.api.distmarker.OnlyIn': ('net.minecraftforge.fml.relauncher.SideOnly', 'SideOnly'),
    'net.minecraftforge.api.distmarker.Dist': ('net.minecraftforge.fml.relauncher.Side', 'Side'),
}

# plain text replacements applied after type renames (regex -> replacement)
CALLS = [
    (r'\bMinecraft\.getInstance\(\)\.renderBuffers\(\)\.bufferSource\(\)', 'RenderBuffers.bufferSource()'),
    (r'\bMinecraft\.getInstance\(\)', 'Minecraft.getMinecraft()'),
    (r'\bViewportEvent\.ComputeCameraAngles\b', 'EntityViewRenderEvent.CameraSetup'),
    (r'\bViewportEvent\.ComputeFov\b', 'EntityViewRenderEvent.FOVModifier'),
    (r'\bViewportEvent\.RenderFog\b', 'EntityViewRenderEvent.RenderFogEvent'),
    (r'\bMinecraft\.ON_OSX\b', 'Minecraft.IS_RUNNING_ON_MAC'),
    (r'\bMinecraft\.getMinecraft\(\)\.font\b', 'Minecraft.getMinecraft().fontRenderer'),
    (r'\bMinecraft\.getMinecraft\(\)\.getFrameTime\(\)', 'Minecraft.getMinecraft().getRenderPartialTicks()'),
    (r'\bMinecraft\.getMinecraft\(\)\.getPartialTick\(\)', 'Minecraft.getMinecraft().getRenderPartialTicks()'),
    (r'\bI18n\.get\(', 'I18n.format('),
    (r'\bItemStack\.matches\(', 'ItemStack.areItemStacksEqual('),
    (r'\bItemStack\.isSameItem\(', 'ItemStack.areItemsEqual('),
    (r'\bItemStack\.isSameItemSameTags\(', 'ItemStack.areItemStacksEqual('),
]


# static imports of upstream enums / holders -> 1.12.2 owner
STATIC_OWNERS = {
    'net.minecraft.world.item.ItemDisplayContext': 'net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType',
}

SWAY_1_20 = '''float xRotOffset = MathHelper.lerp(partialTick, player.xBobO, player.xBob);
            float yRotOffset = MathHelper.lerp(partialTick, player.yBobO, player.yBob);
            float xRot = player.getViewXRot(partialTick) - xRotOffset;
            float yRot = player.getViewYRot(partialTick) - yRotOffset;'''
SWAY_1_12 = '''// 1.12.2: the same terms vanilla ItemRenderer#rotateArm put on the GL matrix (xBob / yBob there)
            float xRotOffset = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTick;
            float yRotOffset = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTick;
            float xRot = player.rotationPitch - xRotOffset;
            float yRot = player.rotationYaw - yRotOffset;'''


def port_source(text):
    simple_renames = {}
    out_lines = []
    for line in text.split('\n'):
        sm = re.match(r'^import static ([\w.]+)\.(\*|\w+);\s*$', line)
        if sm and sm.group(1) in STATIC_OWNERS:
            line = 'import static %s.%s;' % (STATIC_OWNERS[sm.group(1)], sm.group(2))
        m = re.match(r'^import (static )?([\w.]+)(\.\*)?;\s*$', line)
        if m and not m.group(1) and not m.group(3):
            fqn = m.group(2)
            if fqn in TYPES:
                target = TYPES[fqn]
                new_fqn, new_simple = (target if isinstance(target, tuple) else (target, None))
                old_simple = fqn.rsplit('.', 1)[1]
                if new_simple and new_simple != old_simple:
                    simple_renames[old_simple] = new_simple
                line = 'import %s;' % new_fqn
        out_lines.append(line)
    # drop duplicate imports created by many-to-one mappings
    seen = set()
    deduped = []
    for line in out_lines:
        if line.startswith('import '):
            if line in seen:
                continue
            seen.add(line)
        deduped.append(line)
    body = '\n'.join(deduped)
    header_end = 0
    for m in re.finditer(r'^import .*;$', body, re.M):
        header_end = m.end()
    head, rest = body[:header_end], body[header_end:]
    for old, new in simple_renames.items():
        rest = re.sub(r'(?<![\w.])%s\b' % re.escape(old), new, rest)
    for pattern, repl in CALLS:
        rest = re.sub(pattern, repl, rest)
    rest = rest.replace(SWAY_1_20, SWAY_1_12)
    # Optional#ifPresentOrElse (Java 9) on a single-line receiver
    rest, n = re.subn(r'([\w.]+(?:\([^()]*\))?)\.ifPresentOrElse\(', r'Optionals.ifPresentOrElse(\1, ', rest)
    if n and 'import com.tacz.guns.legacy.util.Optionals;' not in head:
        head += '\nimport com.tacz.guns.legacy.util.Optionals;'
    rest = re.sub(r'\b(\w+)\.firstPerson\(\)', r'DisplayContext.firstPerson(\1)', rest)
    if 'DisplayContext.firstPerson(' in rest and 'import com.tacz.guns.legacy.render.DisplayContext;' not in head:
        head += '\nimport com.tacz.guns.legacy.render.DisplayContext;'
    if 'RenderBuffers.bufferSource()' in rest and 'import com.tacz.guns.legacy.render.RenderBuffers;' not in head:
        head += '\nimport com.tacz.guns.legacy.render.RenderBuffers;'
    rest = rest.replace('@OnlyIn(Dist.CLIENT)', '@SideOnly(Side.CLIENT)')
    rest = rest.replace('@OnlyIn(Side.CLIENT)', '@SideOnly(Side.CLIENT)')
    rest = rest.replace('@OnlyIn(Dist.DEDICATED_SERVER)', '@SideOnly(Side.SERVER)')
    return head + rest


def main(argv):
    force = '--force' in argv
    to_stdout = '--stdout' in argv
    paths = [a for a in argv if not a.startswith('--')]
    if not paths:
        print(__doc__)
        return 1
    status = 0
    for rel in paths:
        rel = rel.removeprefix('com/tacz/guns/')
        src = UPSTREAM / rel
        dst = TARGET / rel
        if not src.is_file():
            print('missing upstream file: %s' % src, file=sys.stderr)
            status = 1
            continue
        ported = port_source(src.read_text(encoding='utf-8'))
        if to_stdout:
            sys.stdout.write(ported)
            continue
        if dst.exists() and not force:
            print('exists, skipped (use --force): %s' % dst.relative_to(ROOT), file=sys.stderr)
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(ported, encoding='utf-8')
        print(dst.relative_to(ROOT))
    return status


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
