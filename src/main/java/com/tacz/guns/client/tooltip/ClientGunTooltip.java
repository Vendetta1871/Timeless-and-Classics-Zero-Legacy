package com.tacz.guns.client.tooltip;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.client.input.RefitKey;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.client.resource.pojo.display.gun.AmmoCountStyle;
import com.tacz.guns.client.resource.pojo.display.gun.DamageStyle;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.GunTooltipPart;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.ClientTooltipComponent;
import com.tacz.guns.legacy.util.FontUtil;
import com.tacz.guns.legacy.util.MutableComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import static com.tacz.guns.item.ModernKineticGunItem.DefaultPropertyModification.SLUGS;
import com.tacz.guns.legacy.util.Components;

public class ClientGunTooltip implements ClientTooltipComponent {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##%");
    private static final DecimalFormat FORMAT_P_D1 = new DecimalFormat("#.#%");
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat CURRENT_AMMO_FORMAT_PERCENT = new DecimalFormat("0%");

    private final ItemStack gun;
    private final IGun iGun;
    private final CommonGunIndex gunIndex;
    private final @Nullable GunDisplayInstance display;
    private final ItemStack ammo;
    private @Nullable List<String> desc;
    private ITextComponent ammoName;
    private MutableComponent ammoCountText;
    private @Nullable MutableComponent gunType;
    private MutableComponent damage;
    private MutableComponent armorIgnore;
    private MutableComponent headShotMultiplier;
    private MutableComponent weight;
    private MutableComponent tips;
    private MutableComponent levelInfo;
    private @Nullable MutableComponent packInfo;

    private int maxWidth;

    public ClientGunTooltip(GunTooltip tooltip) {
        this.gun = tooltip.getGun();
        this.iGun = tooltip.getIGun();
        ResourceLocation ammoId = tooltip.getAmmoId();
        this.gunIndex = tooltip.getGunIndex();
        this.display = TimelessAPI.getGunDisplay(gun).orElse(null);
        this.ammo = AmmoItemBuilder.create().setId(ammoId).build();
        this.maxWidth = 0;
        this.getText();
    }

    @Override
    public int getHeight() {
        int height = 0;
        if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            height += 10 * this.desc.size() + 2;
        }
        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            height += 24;
        }
        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            height += 34;
        }
        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            height += 34;
        }
        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            height += 14;
        }
        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            height += 14;
        }
        return height;
    }

    @Override
    public int getWidth(FontRenderer font) {
        return this.maxWidth;
    }

    private void getText() {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        BulletData bulletData = gunIndex.getBulletData();
        GunData gunData = gunIndex.getGunData();

        if (shouldShow(GunTooltipPart.DESCRIPTION)) {
            @Nullable String tooltip = gunIndex.getPojo().getTooltip();
            if (tooltip != null) {
                List<String> split = FontUtil.split(font, Components.translatable(tooltip), 300);
                if (split.size() > 3) {
                    this.desc = split.subList(0, 3);
                } else {
                    this.desc = split;
                }
                for (String sequence : this.desc) {
                    this.maxWidth = Math.max(FontUtil.width(font, sequence), this.maxWidth);
                }
            }
        }


        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            this.ammoName = Components.literal(ammo.getDisplayName());
            this.maxWidth = Math.max(FontUtil.width(font, this.ammoName) + 22, this.maxWidth);

            int barrelBulletAmount = (iGun.hasBulletInBarrel(gun) && gunIndex.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
            int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gun, gunIndex.getGunData()) + barrelBulletAmount;
            int currentAmmoCount = iGun.getCurrentAmmoCount(this.gun) + barrelBulletAmount;

            if (!iGun.useDummyAmmo(gun)) {
                if (display != null && display.getAmmoCountStyle()== AmmoCountStyle.PERCENT) {
                    this.ammoCountText = Components.literal(CURRENT_AMMO_FORMAT_PERCENT.format((float) currentAmmoCount / (maxAmmoCount == 0 ? 1f : maxAmmoCount)));
                } else {
                    this.ammoCountText = Components.literal(String.format("%d/%d", currentAmmoCount, maxAmmoCount));
                }
            } else {
                int dummyAmmoAmount = iGun.getDummyAmmoAmount(gun);
                if (display != null && display.getAmmoCountStyle()== AmmoCountStyle.PERCENT) {
                    String p = CURRENT_AMMO_FORMAT_PERCENT.format((float) currentAmmoCount / (maxAmmoCount == 0 ? 1f : maxAmmoCount));
                    this.ammoCountText = Components.literal(String.format("%s (%d)", p, dummyAmmoAmount));
                } else {
                    this.ammoCountText = Components.literal(String.format("%d/%d (%d)", currentAmmoCount, maxAmmoCount, dummyAmmoAmount));
                }

            }
            if (iGun.useInventoryAmmo(gun)) {
                this.ammoCountText = Components.translatable("tooltip.tacz.gun.inventory_mode").withStyle(TextFormatting.YELLOW);
            }
            this.maxWidth = Math.max(FontUtil.width(font, this.ammoCountText) + 22, this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            int expToNextLevel = iGun.getExpToNextLevel(gun);
            int expCurrentLevel = iGun.getExpCurrentLevel(gun);
            int level = iGun.getLevel(gun);
            if (level >= iGun.getMaxLevel()) {
                String levelText = String.format("%d (MAX)", level);
                this.levelInfo = Components.translatable("tooltip.tacz.gun.level").append(Components.literal(levelText).withStyle(TextFormatting.DARK_PURPLE));
            } else {
                String levelText = String.format("%d (%.1f%%)", level, expCurrentLevel / (expToNextLevel + expCurrentLevel) * 100f);
                this.levelInfo = Components.translatable("tooltip.tacz.gun.level").append(Components.literal(levelText).withStyle(TextFormatting.YELLOW));
            }
            this.maxWidth = Math.max(FontUtil.width(font, this.levelInfo), this.maxWidth);

            String tabKey = "tacz.type." + gunIndex.getType() + ".name";
            this.gunType = Components.translatable("tooltip.tacz.gun.type").append(Components.translatable(tabKey).withStyle(TextFormatting.AQUA));
            this.maxWidth = Math.max(FontUtil.width(font, this.gunType), this.maxWidth);

            double damage = AttachmentDataUtils.getDamageWithAttachment(gun, gunData);
            boolean hasSlugInstalled = AllowAttachmentTagMatcher.matchTag(SLUGS, iGun.getAttachmentId(gun, AttachmentType.EXTENDED_MAG));
            int bulletAmount = hasSlugInstalled ? 1 : gunData.getBulletData().getBulletAmount();
            MutableComponent value;
            if (display != null && display.getDamageStyle() == DamageStyle.PER_PROJECTILE && bulletAmount > 1) {
                value = Components.literal(DAMAGE_FORMAT.format(damage/bulletAmount) + "x" + bulletAmount).withStyle(TextFormatting.AQUA);
            } else {
                value = Components.literal(DAMAGE_FORMAT.format(damage)).withStyle(TextFormatting.AQUA);
            }
            if (bulletData.getExplosionData() != null && (AttachmentDataUtils.isExplodeEnabled(gun, gunData) || bulletData.getExplosionData().isExplode())) {
                value.append(" + ").append(DAMAGE_FORMAT.format(bulletData.getExplosionData().getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get())).append(Components.translatable("tooltip.tacz.gun.explosion"));
            }
            this.damage = Components.translatable("tooltip.tacz.gun.damage").append(value);
            this.maxWidth = Math.max(FontUtil.width(font, this.damage), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            @Nullable ExtraDamage extraDamage = bulletData.getExtraDamage();
            if (extraDamage != null) {
                double armorDamagePercent = AttachmentDataUtils.getArmorIgnoreWithAttachment(gun, gunData);
                double headShotMultiplierPercent = AttachmentDataUtils.getHeadshotMultiplier(gun, gunData);

                armorDamagePercent = MathHelper.clamp(armorDamagePercent, 0.0F, 1.0F);

                this.armorIgnore = Components.translatable("tooltip.tacz.gun.armor_ignore", FORMAT.format(armorDamagePercent));
                this.headShotMultiplier = Components.translatable("tooltip.tacz.gun.head_shot_multiplier", FORMAT.format(headShotMultiplierPercent));
            } else {
                this.armorIgnore = Components.translatable("tooltip.tacz.gun.armor_ignore", FORMAT.format(0));
                this.headShotMultiplier = Components.translatable("tooltip.tacz.gun.head_shot_multiplier", FORMAT.format(1));
            }

            double weightFactor = SyncConfig.WEIGHT_SPEED_MULTIPLIER.get();
            double weight = AttachmentDataUtils.getWightWithAttachment(gun, gunData);
            this.weight = Components.translatable("tooltip.tacz.gun.movement_speed", FORMAT_P_D1.format(-weightFactor * weight)).withStyle(TextFormatting.RED);

            this.maxWidth = Math.max(FontUtil.width(font, this.armorIgnore), this.maxWidth);
            this.maxWidth = Math.max(FontUtil.width(font, this.headShotMultiplier), this.maxWidth);
            this.maxWidth = Math.max(FontUtil.width(font, this.weight), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            String keyName = RefitKey.REFIT_KEY.getDisplayName().toUpperCase(Locale.ENGLISH);
            this.tips = Components.translatable("tooltip.tacz.gun.tips", keyName).withStyle(TextFormatting.YELLOW).withStyle(TextFormatting.ITALIC);
            this.maxWidth = Math.max(FontUtil.width(font, this.tips), this.maxWidth);
        }


        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            ResourceLocation gunId = iGun.getGunId(gun);
            PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(gunId);
            if (packInfoObject != null) {
                packInfo = Components.translatable(packInfoObject.getName()).withStyle(TextFormatting.BLUE).withStyle(TextFormatting.ITALIC);
                this.maxWidth = Math.max(FontUtil.width(font, this.packInfo), this.maxWidth);
            }
        }
    }

    @Override
    public void renderText(FontRenderer font, int pX, int pY, GuiGraphics graphics) {
        int yOffset = pY;

        if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
            yOffset += 2;
            for (String sequence : this.desc) {
                graphics.drawString(font, sequence, pX, yOffset, 0xaaaaaa, false);
                yOffset += 10;
            }
        }


        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            yOffset += 4;

            // 弹药名
            graphics.drawString(font, this.ammoName, pX + 20, yOffset, 0xffaa00, false);

            // 弹药数
            graphics.drawString(font, this.ammoCountText, pX + 20, yOffset + 10, 0x777777, false);

            yOffset += 20;
        }


        if (shouldShow(GunTooltipPart.BASE_INFO)) {
            yOffset += 4;

            // 等级信息
            graphics.drawString(font, this.levelInfo, pX, yOffset, 0x777777, false);
            yOffset += 10;

            // 枪械类型
            if (this.gunType != null) {
                graphics.drawString(font, this.gunType, pX, yOffset, 0x777777, false);
                yOffset += 10;
            }

            // 伤害
            graphics.drawString(font, this.damage, pX, yOffset, 0x777777, false);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.EXTRA_DAMAGE_INFO)) {
            yOffset += 4;

            // 穿甲伤害
            graphics.drawString(font, this.armorIgnore, pX, yOffset, 0xffaa00, false);
            yOffset += 10;

            // 爆头伤害
            graphics.drawString(font, this.headShotMultiplier, pX, yOffset, 0xffaa00, false);
            yOffset += 10;

            graphics.drawString(font, this.weight, pX, yOffset, 0xffffff, false);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.UPGRADES_TIP)) {
            yOffset += 4;

            // Z 键说明
            graphics.drawString(font, this.tips, pX, yOffset, 0xffffff, false);
            yOffset += 10;
        }


        if (shouldShow(GunTooltipPart.PACK_INFO)) {
            // 枪包名
            if (packInfo != null) {
                yOffset += 4;
                graphics.drawString(font, this.packInfo, pX, yOffset, 0xffffff, false);
            }
        }
    }

    @Override
    public void renderImage(FontRenderer pFont, int pX, int pY, GuiGraphics guiGraphics) {
        IGun iGun = IGun.getIGunOrNull(this.gun);
        if (iGun == null) {
            return;
        }
        if (shouldShow(GunTooltipPart.AMMO_INFO)) {
            int yOffset = pY;
            if (shouldShow(GunTooltipPart.DESCRIPTION) && this.desc != null) {
                yOffset += this.desc.size() * 10 + 2;
            }
            guiGraphics.renderItem(ammo, pX, yOffset + 4);
        }
    }

    private boolean shouldShow(GunTooltipPart part) {
        return (GunTooltipPart.getHideFlags(this.gun) & part.getMask()) == 0;
    }
}
