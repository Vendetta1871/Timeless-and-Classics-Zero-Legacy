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
    'net.minecraft.network.FriendlyByteBuf': 'com.tacz.guns.legacy.network.FriendlyByteBuf',
    'net.minecraftforge.network.NetworkEvent': 'com.tacz.guns.legacy.network.NetworkEvent',
    'net.minecraftforge.network.NetworkDirection': 'com.tacz.guns.legacy.network.NetworkDirection',
    'net.minecraftforge.network.NetworkRegistry': 'com.tacz.guns.legacy.network.NetworkRegistry',
    'net.minecraftforge.network.PacketDistributor': 'com.tacz.guns.legacy.network.PacketDistributor',
    'net.minecraftforge.network.simple.SimpleChannel': 'com.tacz.guns.legacy.network.SimpleChannel',
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
    'net.minecraftforge.fml.LogicalSide': ('net.minecraftforge.fml.relauncher.Side', 'Side'),
    'net.minecraftforge.eventbus.api.SubscribeEvent': 'net.minecraftforge.fml.common.eventhandler.SubscribeEvent',
    'net.minecraftforge.eventbus.api.Event': 'net.minecraftforge.fml.common.eventhandler.Event',
    'net.minecraftforge.eventbus.api.Cancelable': 'net.minecraftforge.fml.common.eventhandler.Cancelable',
    'net.minecraftforge.eventbus.api.EventPriority': 'net.minecraftforge.fml.common.eventhandler.EventPriority',
    'net.minecraftforge.event.TickEvent': 'net.minecraftforge.fml.common.gameevent.TickEvent',
    # 1.12-only helpers (com.tacz.guns.legacy.util)
    'net.minecraft.util.GsonHelper': 'com.tacz.guns.legacy.util.GsonHelper',
    'net.minecraft.client.gui.GuiGraphics': 'com.tacz.guns.legacy.render.GuiGraphics',
    'com.mojang.blaze3d.platform.Window': 'com.tacz.guns.legacy.render.Window',
    'net.minecraftforge.client.event.InputEvent': 'com.tacz.guns.legacy.client.InputEvent',
    'net.minecraftforge.registries.DeferredRegister': 'com.tacz.guns.legacy.registry.DeferredRegister',
    'net.minecraftforge.registries.RegistryObject': 'com.tacz.guns.legacy.registry.RegistryObject',
    'net.minecraftforge.registries.ForgeRegistries': 'net.minecraftforge.fml.common.registry.ForgeRegistries',
    'net.minecraftforge.eventbus.api.IEventBus': ('net.minecraftforge.fml.common.eventhandler.EventBus', 'EventBus'),
    'net.minecraftforge.fml.loading.FMLPaths': 'com.tacz.guns.legacy.util.FMLPaths',
    'it.unimi.dsi.fastutil.Pair': 'com.tacz.guns.legacy.util.Pair',
    'net.minecraft.Util': 'com.tacz.guns.legacy.util.Util',
    'net.minecraft.world.level.ClipContext': 'com.tacz.guns.legacy.util.ClipContext',
    # more vanilla types with a 1:1 counterpart
    'net.minecraft.network.chat.Style': 'net.minecraft.util.text.Style',
    'net.minecraft.world.level.BlockGetter': ('net.minecraft.world.IBlockAccess', 'IBlockAccess'),
    'net.minecraft.world.entity.player.Inventory': ('net.minecraft.entity.player.InventoryPlayer', 'InventoryPlayer'),
    'net.minecraft.world.Container': ('net.minecraft.inventory.IInventory', 'IInventory'),
    'net.minecraft.world.inventory.Slot': 'net.minecraft.inventory.Slot',
    'net.minecraft.world.level.block.SoundType': 'net.minecraft.block.SoundType',
    'net.minecraft.world.level.block.Blocks': 'net.minecraft.init.Blocks',
    'net.minecraft.sounds.SoundEvents': 'net.minecraft.init.SoundEvents',
    'net.minecraft.world.level.Explosion': 'net.minecraft.world.Explosion',
    'net.minecraft.world.level.GameRules': 'net.minecraft.world.GameRules',
    'net.minecraft.world.entity.item.ItemEntity': ('net.minecraft.entity.item.EntityItem', 'EntityItem'),
    'net.minecraft.world.effect.MobEffect': ('net.minecraft.potion.Potion', 'Potion'),
    'net.minecraft.world.effect.MobEffectInstance': ('net.minecraft.potion.PotionEffect', 'PotionEffect'),
    'net.minecraft.world.entity.ai.attributes.Attribute': ('net.minecraft.entity.ai.attributes.IAttribute', 'IAttribute'),
    'net.minecraft.world.entity.ai.attributes.AttributeModifier': 'net.minecraft.entity.ai.attributes.AttributeModifier',
    'net.minecraft.world.item.enchantment.EnchantmentHelper': 'net.minecraft.enchantment.EnchantmentHelper',
    'net.minecraft.world.item.CreativeModeTab': ('net.minecraft.creativetab.CreativeTabs', 'CreativeTabs'),
    'net.minecraft.world.item.TooltipFlag': ('net.minecraft.client.util.ITooltipFlag', 'ITooltipFlag'),
    'net.minecraft.network.protocol.Packet': 'net.minecraft.network.Packet',
    'net.minecraft.client.player.Input': ('net.minecraft.util.MovementInput', 'MovementInput'),
    'net.minecraft.client.model.HumanoidModel': ('net.minecraft.client.model.ModelBiped', 'ModelBiped'),
    'net.minecraft.client.model.PlayerModel': ('net.minecraft.client.model.ModelPlayer', 'ModelPlayer'),
    'net.minecraft.client.model.geom.ModelPart': ('net.minecraft.client.model.ModelRenderer', 'ModelRenderer'),
    'net.minecraft.client.renderer.entity.player.PlayerRenderer': ('net.minecraft.client.renderer.entity.RenderPlayer', 'RenderPlayer'),
    'net.minecraft.client.resources.sounds.SoundInstance': ('net.minecraft.client.audio.ISound', 'ISound'),
    'net.minecraft.client.resources.sounds.AbstractSoundInstance': ('net.minecraft.client.audio.PositionedSound', 'PositionedSound'),
    'net.minecraft.client.resources.sounds.AbstractTickableSoundInstance': ('net.minecraft.client.audio.MovingSound', 'MovingSound'),
    'net.minecraft.client.resources.sounds.Sound': 'net.minecraft.client.audio.Sound',
    'net.minecraft.client.sounds.WeighedSoundEvents': ('net.minecraft.client.audio.SoundEventAccessor', 'SoundEventAccessor'),
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
    (r'\b(?:ITextComponent|Component)\.translatable\(', 'Components.translatable('),
    (r'\b(?:ITextComponent|Component)\.literal\(', 'Components.literal('),
    (r'\b(?:ITextComponent|Component)\.empty\(\)', 'Components.literal("")'),
    (r'\.getTag\(\)', '.getTagCompound()'),
    (r'\.hasTag\(\)', '.hasTagCompound()'),
    (r'\.setTag\(', '.setTagCompound('),
    (r'\.getMainHandItem\(\)', '.getHeldItemMainhand()'),
    (r'\.getOffhandItem\(\)', '.getHeldItemOffhand()'),
    (r'\.getItemInHand\(', '.getHeldItem('),
    (r'\.level\(\)', '.world'),
    (r'\.isClientSide\(\)', '.isRemote'),
    (r'\.isClientSide\b', '.isRemote'),
    (r'\bMinecraft\.getMinecraft\(\)\.level\b', 'Minecraft.getMinecraft().world'),
    (r'\.isMemoryConnection\(\)', '.isLocalChannel()'),
    (r'\bMinecraft\.getMinecraft\(\)\.getWindow\(\)', 'Window.get()'),
    (r'\bMinecraft\.getMinecraft\(\)\.options\b', 'Minecraft.getMinecraft().gameSettings'),
    (r'\.getCameraType\(\)\.isFirstPerson\(\)', '.thirdPersonView == 0'),
    (r'\bMinecraft\.getMinecraft\(\)\.hitResult\b', 'Minecraft.getMinecraft().objectMouseOver'),
    (r'\bMinecraft\.getMinecraft\(\)\.gameMode\b', 'Minecraft.getMinecraft().playerController'),
    (r'\.getBoundingBox\(\)', '.getEntityBoundingBox()'),
    (r'\.isCrouching\(\)', '.isSneaking()'),
    (r'\.isShiftKeyDown\(\)', '.isSneaking()'),
    (r'\.getVehicle\(\)', '.getRidingEntity()'),
    (r'\.expandTowards\(', '.expand('),
    (r'\.inflate\(', '.grow('),
    (r'\bMinecraft\.getMinecraft\(\)\.cameraEntity\b', 'Minecraft.getMinecraft().getRenderViewEntity()'),
    (r'\.walkDistO\b', '.prevDistanceWalkedModified'),
    (r'\.walkDist\b', '.distanceWalkedModified'),
    (r'\.input\.up\b', '.movementInput.forwardKeyDown'),
    (r'\.input\.down\b', '.movementInput.backKeyDown'),
    (r'\.input\.left\b', '.movementInput.leftKeyDown'),
    (r'\.input\.right\b', '.movementInput.rightKeyDown'),
    (r'\.input\.jumping\b', '.movementInput.jump'),
    (r'\.input\.shiftKeyDown\b', '.movementInput.sneak'),
    (r'\.getUUID\(\)', '.getUniqueID()'),
    (r'\.isRemoved\(\)', '.isDead'),
    (r'\.getRandom\(\)', '.getRNG()'),
    (r'\.getResourceManager\(\)\.open\(([^()]*)\)', r'.getResourceManager().getResource(\1).getInputStream()'),
    (r'\bJsonParser\.parseReader\(', 'new JsonParser().parse('),
    (r'\bJsonParser\.parseString\(', 'new JsonParser().parse('),
    (r'\bObjects\.requireNonNullElseGet\(', 'Optionals.requireNonNullElseGet('),
    (r'("(?:[^"\\]|\\.)*")\.formatted\(', r'String.format(\1, '),
    (r'\bnew JsonParser\(\)\.parse\(|\bJsonParser\.parse(?:String|Reader)\(', 'new JsonParser().parse('),
    (r'\.isDeadOrDying\(\)', '.getHealth() <= 0'),
    (r'\bMinecraft\.getMinecraft\(\)\.screen\b', 'Minecraft.getMinecraft().currentScreen'),
    (r'\.getInventory\(\)', '.inventory'),
    (r'\binventory\.selected\b', 'inventory.currentItem'),
    (r'\bObjects\.requireNonNullElse\(', 'MoreObjects.firstNonNull('),
    (r'\.yOld\b', '.lastTickPosY'),
    (r'\.xOld\b', '.lastTickPosX'),
    (r'\.zOld\b', '.lastTickPosZ'),
    (r'\.onGround\(\)', '.onGround'),
    (r'\bMinecraft\.getMinecraft\(\)\.options\.bobView\(\)\.get\(\)', 'Minecraft.getMinecraft().gameSettings.viewBobbing'),
    (r'\.getAbilities\(\)\.flying\b', '.capabilities.isFlying'),
    (r'\.getAbilities\(\)\.instabuild\b', '.capabilities.isCreativeMode'),
    (r'\bSoundEvent\.createVariableRangeEvent\(', 'new SoundEvent('),
    (r'\bForgeRegistries\.ENTITY_TYPES\b', 'ForgeRegistries.ENTITIES'),
    (r'\bForgeRegistries\.MOB_EFFECTS\b', 'ForgeRegistries.POTIONS'),
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



def _split_params(text):
    """Splits 'A<B, C> a, @X D d' on top-level commas."""
    parts, depth, cur = [], 0, ''
    for ch in text:
        if ch in '<([':
            depth += 1
        elif ch in '>)]':
            depth -= 1
        if ch == ',' and depth == 0:
            parts.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        parts.append(cur.strip())
    return parts


def _strip_annotations(param):
    return re.sub(r'@\w+(\([^)]*\))?\s*', '', param).strip()


RECORD_RE = re.compile(r'^(?P<indent>[ \t]*)(?P<mods>(?:(?:public|protected|private|static|final)\s+)*)record\s+'
                       r'(?P<name>\w+)(?P<generics><[^>{]*(?:<[^>{]*>[^>{]*)*>)?\s*\((?P<params>[^{]*?)\)\s*'
                       r'(?P<impl>implements\s+[^{]+)?\{', re.M)


def derecord(text):
    """Converts simple records (no compact constructor) into final classes with record-style accessors."""
    out, pos = [], 0
    for m in RECORD_RE.finditer(text):
        params = [_strip_annotations(x) for x in _split_params(m.group('params'))]
        fields = []
        for prm in params:
            ptype, pname = prm.rsplit(None, 1)
            fields.append((ptype.strip(), pname.strip()))
        ind = m.group('indent')
        i2 = ind + '    '
        i3 = i2 + '    '
        name = m.group('name')
        generics = m.group('generics') or ''
        mods = m.group('mods')
        if 'static' not in mods and ind:
            mods = mods + 'static '
        impl = (m.group('impl') or '').strip()
        head = '%s%sfinal class %s%s %s{\n' % (ind, mods, name, generics, (impl + ' ') if impl else '')
        body = ''
        for t, n in fields:
            body += '%sprivate final %s %s;\n' % (i2, t, n)
        body += '\n%spublic %s(%s) {\n' % (i2, name, ', '.join('%s %s' % f for f in fields))
        for t, n in fields:
            body += '%sthis.%s = %s;\n' % (i3, n, n)
        body += '%s}\n' % i2
        for t, n in fields:
            body += '\n%spublic %s %s() {\n%sreturn %s;\n%s}\n' % (i2, t, n, i3, n, i2)
        if fields:
            eq = ' && '.join('java.util.Objects.equals(%s, that.%s)' % (n, n) for t, n in fields)
            body += ('\n%s@Override\n%spublic boolean equals(Object o) {\n%sif (this == o) return true;\n'
                     '%sif (!(o instanceof %s)) return false;\n%s%s<?> that = (%s<?>) o;\n%sreturn %s;\n%s}\n') % (
                i2, i2, i3, i3, name, i3, name, name, i3, eq, i2) if generics else (
                '\n%s@Override\n%spublic boolean equals(Object o) {\n%sif (this == o) return true;\n'
                '%sif (!(o instanceof %s)) return false;\n%s%s that = (%s) o;\n%sreturn %s;\n%s}\n') % (
                i2, i2, i3, i3, name, i3, name, name, i3, eq, i2)
            body += '\n%s@Override\n%spublic int hashCode() {\n%sreturn java.util.Objects.hash(%s);\n%s}\n' % (
                i2, i2, i3, ', '.join(n for t, n in fields), i2)
            ts = ' + ", " + '.join('"%s=" + %s' % (n, n) for t, n in fields)
            body += '\n%s@Override\n%spublic String toString() {\n%sreturn "%s[" + %s + "]";\n%s}\n' % (
                i2, i2, i3, name, ts, i2)
        out.append(text[pos:m.start()])
        out.append(head + body)
        pos = m.end()
        # keep the record's own body (members) after the generated ones
        rest_start = pos
        # drop a lone newline so an empty record body closes cleanly
    out.append(text[pos:])
    return ''.join(out)

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
    # Mth helpers that 1.12.2's MathHelper lacks
    rest, n = re.subn(r'\bMathHelper\.(lerp|inverseLerp|square|sign)\(', r'LegacyMath.\1(', rest)
    if n and 'import com.tacz.guns.legacy.util.LegacyMath;' not in head:
        head += '\nimport com.tacz.guns.legacy.util.LegacyMath;'
    # Optional#ifPresentOrElse (Java 9) on a single-line receiver
    rest, n = re.subn(r'([\w.]+(?:\([^()]*\))?)\.ifPresentOrElse\(', r'Optionals.ifPresentOrElse(\1, ', rest)
    if (n or 'Optionals.' in rest) and 'import com.tacz.guns.legacy.util.Optionals;' not in head:
        head += '\nimport com.tacz.guns.legacy.util.Optionals;'
    rest = re.sub(r'\b(\w+)\.firstPerson\(\)', r'DisplayContext.firstPerson(\1)', rest)
    if 'DisplayContext.firstPerson(' in rest and 'import com.tacz.guns.legacy.render.DisplayContext;' not in head:
        head += '\nimport com.tacz.guns.legacy.render.DisplayContext;'
    if 'Window.get()' in rest and 'import com.tacz.guns.legacy.render.Window;' not in head:
        head += '\nimport com.tacz.guns.legacy.render.Window;'
    if 'Components.' in rest and 'import com.tacz.guns.legacy.util.Components;' not in head:
        head += '\nimport com.tacz.guns.legacy.util.Components;'
    if 'RenderBuffers.bufferSource()' in rest and 'import com.tacz.guns.legacy.render.RenderBuffers;' not in head:
        head += '\nimport com.tacz.guns.legacy.render.RenderBuffers;'
    rest = rest.replace('@OnlyIn(Dist.CLIENT)', '@SideOnly(Side.CLIENT)')
    rest = rest.replace('@OnlyIn(Side.CLIENT)', '@SideOnly(Side.CLIENT)')
    rest = rest.replace('@OnlyIn(Dist.DEDICATED_SERVER)', '@SideOnly(Side.SERVER)')
    rest = derecord(rest)
    # IClientItemExtensions.of(item or stack).getCustomRenderer() -> Item#getTileEntityItemStackRenderer()
    rest = re.sub(r'\bIClientItemExtensions\.of\(([^()]*(?:\([^()]*\))?[^()]*)\)\.getCustomRenderer\(\)',
                  lambda m: m.group(1) + ('' if m.group(1).endswith('getItem()') else '.getItem()') + '.getTileEntityItemStackRenderer()', rest)
    # Java 9+ collection factories -> Guava 21 (same null-hostility and immutability)
    for jdk, guava in (('List', 'ImmutableList'), ('Set', 'ImmutableSet'), ('Map', 'ImmutableMap')):
        rest, n = re.subn(r'(?<![\w.])%s\.(of|copyOf)\(' % jdk, guava + r'.\1(', rest)
        imp = 'import com.google.common.collect.%s;' % guava
        if n and imp not in head:
            head += '\n' + imp
    rest, n = re.subn(r'(?<!Collectors)\.toList\(\)', '.collect(Collectors.toList())', rest)
    if n and 'import java.util.stream.Collectors;' not in head and 'import java.util.stream.*;' not in head:
        head += '\nimport java.util.stream.Collectors;'
    rest = re.sub(r'\.isBlank\(\)', '.trim().isEmpty()', rest)
    if 'MoreObjects.' in rest and 'import com.google.common.base.MoreObjects;' not in head:
        head += '\nimport com.google.common.base.MoreObjects;'
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
