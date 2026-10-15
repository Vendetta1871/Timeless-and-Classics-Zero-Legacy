package com.tacz.guns.client.tooltip;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.gui.GuiScreen;
import com.tacz.guns.legacy.util.ClientTooltipComponent;
import com.tacz.guns.legacy.util.FontUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.tacz.guns.legacy.util.Components;

public class ClientAttachmentItemTooltip implements ClientTooltipComponent {
    private static final Cache<ResourceLocation, List<ItemStack>> CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).build();
    private final ResourceLocation attachmentId;
    private final List<ITextComponent> components = Lists.newArrayList();
    private final ITextComponent tips = Components.translatable("tooltip.tacz.attachment.yaw.shift");
    private final ITextComponent support = Components.translatable("tooltip.tacz.attachment.yaw.support");
    private @Nullable ITextComponent packInfo;
    private List<ItemStack> showGuns = Lists.newArrayList();
    private ItemStack attachment;

    public ClientAttachmentItemTooltip(AttachmentItemTooltip tooltip) {
        this.attachmentId = tooltip.getAttachmentId();
        this.attachment = tooltip.getAttachmentItem();
        this.addText(tooltip.getType());
        this.getShowGuns();
        this.addPackInfo();
    }

    private void addPackInfo() {
        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(attachmentId);
        if (packInfoObject != null) {
            packInfo = Components.translatable(packInfoObject.getName()).withStyle(TextFormatting.BLUE).withStyle(TextFormatting.ITALIC);
        }
    }

    private static List<ItemStack> getAllAllowGuns(List<ItemStack> output, ResourceLocation attachmentId) {
        ItemStack attachment = AttachmentItemBuilder.create().setId(attachmentId).build();
        TimelessAPI.getAllCommonGunIndex().forEach(entry -> {
            ResourceLocation gunId = entry.getKey();
            ItemStack gun = GunItemBuilder.create().setId(gunId).build();
            if (!(gun.getItem() instanceof IGun iGun)) {
                return;
            }
            if (iGun.allowAttachment(gun, attachment)) {
                output.add(gun);
            }
        });
        return output;
    }

    @Override
    public int getHeight() {
        if (!GuiScreen.isShiftKeyDown()) {
            return components.size() * 10 + 28;
        }
        return (showGuns.size() - 1) / 16 * 18 + 50 + components.size() * 10;
    }

    @Override
    public int getWidth(FontRenderer font) {
        int[] width = new int[]{0};
        if (packInfo != null) {
            width[0] = Math.max(width[0], FontUtil.width(font, packInfo) + 4);
        }
        components.forEach(c -> width[0] = Math.max(width[0], FontUtil.width(font, c)));
        if (!GuiScreen.isShiftKeyDown()) {
            return Math.max(width[0], FontUtil.width(font, tips) + 4);
        } else {
            width[0] = Math.max(width[0], FontUtil.width(font, support) + 4);
        }
        if (showGuns.size() > 15) {
            return Math.max(width[0], 260);
        }
        return Math.max(width[0], showGuns.size() * 16 + 4);
    }

    @Override
    public void renderText(FontRenderer font, int pX, int pY, GuiGraphics graphics) {
        int yOffset = pY;
        for (ITextComponent component : this.components) {
            graphics.drawString(font, component, pX, yOffset, 0xffaa00, false);
            yOffset += 10;
        }
        if (!GuiScreen.isShiftKeyDown()) {
            graphics.drawString(font, tips, pX, pY + 5 + this.components.size() * 10, 0x9e9e9e, false);
            yOffset += 10;
        } else {
            yOffset += (showGuns.size() - 1) / 16 * 18 + 32;
        }
        // 枪包名
        if (packInfo != null) {
            graphics.drawString(font, this.packInfo, pX, yOffset + 8, 0xffffff, false);
        }
    }

    @Override
    public void renderImage(FontRenderer font, int mouseX, int mouseY, GuiGraphics gui) {
        if (!GuiScreen.isShiftKeyDown()) {
            return;
        }
        int minY = components.size() * 10 + 3;
        int maxX = getWidth(font);
        gui.fill(mouseX, mouseY + minY, mouseX + maxX, mouseY + minY + 11, 0x8F00b0ff);
        gui.drawString(font, support, mouseX + 2, mouseY + minY + 2, 0xe3f2fd);

        for (int i = 0; i < showGuns.size(); i++) {
            ItemStack stack = showGuns.get(i);
            int x = i % 16 * 16 + 2;
            int y = i / 16 * 18 + minY + 15;
            gui.renderItem(stack, mouseX + x, mouseY + y);
        }
    }

    private void getShowGuns() {
        try {
            this.showGuns = CACHE.get(attachmentId, () -> getAllAllowGuns(Lists.newArrayList(), attachmentId));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static String rgbToHex(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private void addText(AttachmentType type) {
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            AttachmentData data = index.getData();

            @Nullable String tooltipKey = index.getTooltipKey();
            if (tooltipKey != null) {
                String text = I18n.format(tooltipKey);
                String[] split = text.split("\n");
                Arrays.stream(split).forEach(s -> components.add(Components.literal(s).withStyle(TextFormatting.GRAY)));
            }

            if (attachment.getItem() instanceof IAttachment iAttachment) {
                TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(attachmentIndex -> {
                    if (iAttachment.hasCustomLaserColor(attachment)) {
                        int color = iAttachment.getLaserColor(attachment);
                        components.add(Components.translatable("tooltip.tacz.attachment.laser.color", rgbToHex(color)));
                    } else if (attachmentIndex.getLaserConfig() != null) {
                        int color = attachmentIndex.getLaserConfig().getDefaultColor();
                        components.add(Components.translatable("tooltip.tacz.attachment.laser.color", rgbToHex(color)));
                    }
                });
            }

            if (type == AttachmentType.SCOPE) {
                float[] zoom = index.getZoom();
                if (zoom != null) {
                    String[] zoomText = new String[zoom.length];
                    for (int i = 0; i < zoom.length; i++) {
                        zoomText[i] = "x" + zoom[i];
                    }
                    String zoomJoinText = StringUtils.join(zoomText, ", ");
                    components.add(Components.translatable("tooltip.tacz.attachment.zoom", zoomJoinText).withStyle(TextFormatting.GOLD));
                }
            }

            if (type == AttachmentType.EXTENDED_MAG) {
                int magLevel = data.getExtendedMagLevel();
                if (magLevel == 1) {
                    components.add(Components.translatable("tooltip.tacz.attachment.extended_mag_level_1").withStyle(TextFormatting.GRAY));
                } else if (magLevel == 2) {
                    components.add(Components.translatable("tooltip.tacz.attachment.extended_mag_level_2").withStyle(TextFormatting.BLUE));
                } else if (magLevel == 3) {
                    components.add(Components.translatable("tooltip.tacz.attachment.extended_mag_level_3").withStyle(TextFormatting.LIGHT_PURPLE));
                }
            }

            data.getModifier().forEach((key, value) -> {
                List<ITextComponent> result = value.getComponents();
                components.addAll(result);
            });
        });
    }
}
