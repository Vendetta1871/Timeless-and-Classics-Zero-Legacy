package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import com.tacz.guns.legacy.util.Components;

// TODO(port): upstream implements DyeableLeatherItem (dye colour in the "display.color" tag, 1.12.2 has no such interface)
public class AmmoBoxItem extends Item implements AmmoBoxItemDataAccessor {
    public static final ResourceLocation PROPERTY_NAME = new ResourceLocation(GunMod.MOD_ID, "ammo_statue");

    public static final int IRON_LEVEL = 0;
    public static final int GOLD_LEVEL = 1;
    public static final int DIAMOND_LEVEL = 2;

    private static final String DISPLAY_TAG = "display";
    private static final String COLOR_TAG = "color";

    private static final int OPEN = 0;
    private static final int CLOSE = 1;

    private static final int CREATIVE_INDEX = 6;
    private static final int ALL_TYPE_CREATIVE_INDEX = 8;

    public AmmoBoxItem() {
        super();
        this.setMaxStackSize(1);
        this.setTranslationKey(GunMod.MOD_ID + ".ammo_box");
    }

    @SideOnly(Side.CLIENT)
    public static int getColor(ItemStack stack, int tintIndex) {
        return tintIndex > 0 ? -1 : getTagColor(stack);
    }

    @SideOnly(Side.CLIENT)
    public static float getStatue(ItemStack stack, @Nullable World level, @Nullable EntityLivingBase entity) {
        int openStatue = OPEN;
        int ammoLevel = IRON_LEVEL;
        if (stack.getItem() instanceof IAmmoBox iAmmoBox) {
            if (iAmmoBox.isAllTypeCreative(stack)) {
                return ALL_TYPE_CREATIVE_INDEX;
            }
            openStatue = getOpenStatue(stack, iAmmoBox);
            if (iAmmoBox.isCreative(stack)) {
                return openStatue + CREATIVE_INDEX;
            }
            ammoLevel = getLevelStatue(stack, iAmmoBox);
        }
        return openStatue + 2 * ammoLevel;
    }

    private static int getOpenStatue(ItemStack stack, IAmmoBox iAmmoBox) {
        boolean idIsEmpty = iAmmoBox.getAmmoId(stack).equals(DefaultAssets.EMPTY_AMMO_ID);
        boolean countIsZero = iAmmoBox.getAmmoCount(stack) <= 0;
        if (idIsEmpty || countIsZero) {
            return OPEN;
        }
        return CLOSE;
    }

    private static int getLevelStatue(ItemStack stack, IAmmoBox iAmmoBox) {
        return iAmmoBox.getAmmoLevel(stack);
    }

    private static int getTagColor(ItemStack stack) {
        NBTTagCompound compoundtag = stack.getSubCompound(DISPLAY_TAG);
        return compoundtag != null && compoundtag.hasKey(COLOR_TAG, Constants.NBT.TAG_ANY_NUMERIC) ? compoundtag.getInteger(COLOR_TAG) : 0x727d6b;
    }

    // TODO(port): 1.12.2 has no Item#overrideOtherStackedOnMe / overrideStackedOnOther (bundle-like clicks on an ammo box in
    //  an inventory screen); needs a mixin into Container#slotClick or a ClickType hook
    //    @Override
    //    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack pOther, Slot slot, ClickAction action, EntityPlayer player, SlotAccess access) {
    //        return super.overrideOtherStackedOnMe(stack, pOther, slot, action, player, access);
    //    }
    //
    //    @Override
    //    public boolean overrideStackedOnOther(ItemStack ammoBox, Slot slot, ClickAction action, EntityPlayer player) {
    //        // 右击
    //        if (action == ClickAction.SECONDARY) {
    //            // 点击的格子
    //            ItemStack slotItem = slot.getItem();
    //            ResourceLocation boxAmmoId = this.getAmmoId(ammoBox);
    //
    //            // 格子为空，那就是取出物品
    //            if (slotItem.isEmpty()) {
    //                // 创造模式弹药箱不能取出任何东西
    //                if (isAllTypeCreative(ammoBox) || isCreative(ammoBox)) {
    //                    return false;
    //                }
    //                // 啥也没有，不能取出
    //                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
    //                    return false;
    //                }
    //                // 数量不对，不能取出
    //                int boxAmmoCount = this.getAmmoCount(ammoBox);
    //                if (boxAmmoCount <= 0) {
    //                    return false;
    //                }
    //                return TimelessAPI.getCommonAmmoIndex(boxAmmoId).map(index -> {
    //                    int takeCount = Math.min(index.getStackSize(), boxAmmoCount);
    //                    ItemStack takeAmmo = AmmoItemBuilder.create().setId(boxAmmoId).setCount(takeCount).build();
    //                    ItemStack remainingAmmo = slot.safeInsert(takeAmmo);
    //                    int insertedCount = takeCount - remainingAmmo.getCount();
    //                    if (insertedCount <= 0) {
    //                        return false;
    //                    }
    //
    //                    int remainCount = boxAmmoCount - insertedCount;
    //                    this.setAmmoCount(ammoBox, remainCount);
    //                    if (remainCount <= 0) {
    //                        this.setAmmoId(ammoBox, DefaultAssets.EMPTY_AMMO_ID);
    //                    }
    //                    this.playRemoveOneSound(player);
    //                    return true;
    //                }).orElse(false);
    //            }
    //
    //            // 如果是子弹
    //            if (slotItem.getItem() instanceof IAmmo iAmmo) {
    //                // 全类型弹药箱不能存入
    //                if (isAllTypeCreative(ammoBox)) {
    //                    return false;
    //                }
    //                ResourceLocation slotAmmoId = iAmmo.getAmmoId(slotItem);
    //                // 格子里的子弹 ID 不对，不能放
    //                if (slotAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
    //                    return false;
    //                }
    //                // 如果盒子的子弹 ID 为空，变成当前点击的类型
    //                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
    //                    this.setAmmoId(ammoBox, slotAmmoId);
    //                } else if (!slotAmmoId.equals(boxAmmoId)) {
    //                    return false;
    //                }
    //                TimelessAPI.getCommonAmmoIndex(slotAmmoId).ifPresent(index -> {
    //                    // 创造模式弹药箱，那就直接存入最大
    //                    if (isCreative(ammoBox)) {
    //                        this.setAmmoCount(ammoBox, Integer.MAX_VALUE);
    //                        return;
    //                    }
    //                    int boxAmmoCount = this.getAmmoCount(ammoBox);
    //                    int boxLevelMultiplier = this.getAmmoLevel(ammoBox) + 1;
    //                    int maxSize = index.getStackSize() * SyncConfig.AMMO_BOX_STACK_SIZE.get() * boxLevelMultiplier;
    //                    int needCount = maxSize - boxAmmoCount;
    //                    ItemStack takeItem = slot.safeTake(slotItem.getCount(), needCount, player);
    //                    this.setAmmoCount(ammoBox, boxAmmoCount + takeItem.getCount());
    //                });
    //                // 播放取出声音
    //                this.playInsertSound(player);
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    private void playRemoveOneSound(Entity entity) {
        // TODO(port): 1.12.2 has no bundle sounds, closest vanilla sound
        entity.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8F, 0.8F + entity.world.rand.nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        // TODO(port): 1.12.2 has no bundle sounds, closest vanilla sound
        entity.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8F, 0.8F + entity.world.rand.nextFloat() * 0.4F);
    }

    /**
     * 1.12.2: {@code isBarVisible}
     */
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        if (isAllTypeCreative(stack) || isCreative(stack)) {
            return false;
        }
        return !this.getAmmoId(stack).equals(DefaultAssets.EMPTY_AMMO_ID) && this.getAmmoCount(stack) > 0;
    }

    /**
     * 1.12.2: {@code getBarWidth}; the durability bar is the fraction that is missing (upstream's width is 1 - 13)
     */
    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        ResourceLocation ammoId = this.getAmmoId(stack);
        int ammoCount = this.getAmmoCount(stack);
        int boxLevelMultiplier = this.getAmmoLevel(stack) + 1;
        double widthPercent = TimelessAPI.getCommonAmmoIndex(ammoId).map(index -> {
            double totalCount = index.getStackSize() * SyncConfig.AMMO_BOX_STACK_SIZE.get() * boxLevelMultiplier;
            return ammoCount / totalCount;
        }).orElse(0d);
        int barWidth = (int) Math.min(1 + 12 * widthPercent, 13);
        return 1.0D - barWidth / 13.0D;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isAllTypeCreative(stack)) {
            return Components.translatable("item.tacz.ammo_box.all_type_creative").withStyle(TextFormatting.DARK_PURPLE).getFormattedText();
        }
        if (isCreative(stack)) {
            return Components.translatable("item.tacz.ammo_box.creative").withStyle(TextFormatting.DARK_PURPLE).getFormattedText();
        }
        int ammoLevel = getAmmoLevel(stack);
        switch (ammoLevel) {
            case GOLD_LEVEL -> {
                return Components.translatable("item.tacz.ammo_box.gold").withStyle(TextFormatting.YELLOW).getFormattedText();
            }
            case DIAMOND_LEVEL -> {
                return Components.translatable("item.tacz.ammo_box.diamond").withStyle(TextFormatting.AQUA).getFormattedText();
            }
            default -> {
                return Components.translatable("item.tacz.ammo_box.iron").getFormattedText();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        if (isAllTypeCreative(stack) || isCreative(stack)) {
            return true;
        }
        return super.hasEffect(stack);
    }

    /**
     * 1.12.2: {@code getBarColor}
     */
    @Override
    @SideOnly(Side.CLIENT)
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return MathHelper.hsvToRGB(1 / 3f, 1.0F, 1.0F);
    }

    public static void fillItemCategory(NonNullList<ItemStack> output) {
        ItemStack ammoBox = new ItemStack(ModItems.AMMO_BOX.get());
        if (ammoBox.getItem() instanceof IAmmoBox iAmmoBox) {
            // 添加普通版本的弹药盒
            output.add(iAmmoBox.setAmmoLevel(ammoBox.copy(), IRON_LEVEL));
            output.add(iAmmoBox.setAmmoLevel(ammoBox.copy(), GOLD_LEVEL));
            output.add(iAmmoBox.setAmmoLevel(ammoBox.copy(), DIAMOND_LEVEL));

            // 添加创造模式弹药盒
            output.add(iAmmoBox.setCreative(ammoBox.copy(), false));
            output.add(iAmmoBox.setCreative(ammoBox.copy(), true));
        }
    }

    // 1.12.2 has no TooltipComponent / Item#getTooltipImage: AmmoBoxTooltip is a plain data class, the tooltip is drawn by legacy.util.TooltipComponentHandler
    public Optional<AmmoBoxTooltip> getTooltipImage(ItemStack stack) {
        if (!(stack.getItem() instanceof IAmmoBox iAmmoBox)) {
            return Optional.empty();
        }
        ResourceLocation ammoId = iAmmoBox.getAmmoId(stack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
            return Optional.empty();
        }
        int ammoCount = iAmmoBox.getAmmoCount(stack);
        if (ammoCount <= 0) {
            return Optional.empty();
        }
        ItemStack ammoStack = AmmoItemBuilder.create().setId(ammoId).build();
        return Optional.of(new AmmoBoxTooltip(stack, ammoStack, ammoCount));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World pLevel, List<String> components, ITooltipFlag isAdvanced) {
        if (isAllTypeCreative(stack)) {
            components.add(Components.translatable("tooltip.tacz.ammo_box.usage.all_type_creative").withStyle(TextFormatting.GOLD).getFormattedText());
            return;
        }
        if (isCreative(stack)) {
            components.add(Components.translatable("tooltip.tacz.ammo_box.usage.creative.1").withStyle(TextFormatting.YELLOW).getFormattedText());
            components.add(Components.translatable("tooltip.tacz.ammo_box.usage.creative.2").withStyle(TextFormatting.YELLOW).getFormattedText());
            return;
        }
        components.add(Components.translatable("tooltip.tacz.ammo_box.usage.deposit").withStyle(TextFormatting.GRAY).getFormattedText());
        components.add(Components.translatable("tooltip.tacz.ammo_box.usage.remove").withStyle(TextFormatting.GRAY).getFormattedText());
    }
}
