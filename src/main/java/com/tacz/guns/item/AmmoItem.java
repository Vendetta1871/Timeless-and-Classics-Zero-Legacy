package com.tacz.guns.item;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.client.renderer.item.AmmoItemRenderer;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AmmoItem extends Item implements AmmoItemDataAccessor {
    public AmmoItem() {
        super();
        this.setMaxStackSize(1);
        this.setTranslationKey(GunMod.MOD_ID + ".ammo");
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.initializeClient();
        }
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo iAmmo) {
            return TimelessAPI.getCommonAmmoIndex(iAmmo.getAmmoId(stack))
                    .map(CommonAmmoIndex::getStackSize).orElse(1);
        }
        return 1;
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        ResourceLocation ammoId = this.getAmmoId(stack);
        Optional<ClientAmmoIndex> ammoIndex = TimelessAPI.getClientAmmoIndex(ammoId);
        if (ammoIndex.isPresent()) {
            return I18n.format(ammoIndex.get().getName());
        }
        return super.getItemStackDisplayName(stack);
    }

    private static Comparator<Map.Entry<ResourceLocation, CommonAmmoIndex>> idNameSort() {
        return Comparator.comparingInt(m -> m.getValue().getSort());
    }

    public static NonNullList<ItemStack> fillItemCategory() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonAmmoIndex().stream().sorted(idNameSort()).forEach(entry -> {
            ItemStack itemStack = AmmoItemBuilder.create().setId(entry.getKey()).build();
            stacks.add(itemStack);
        });
        return stacks;
    }

    /**
     * 1.12.2: {@code IClientItemExtensions#getCustomRenderer}
     */
    @SideOnly(Side.CLIENT)
    private void initializeClient() {
        this.setTileEntityItemStackRenderer(new AmmoItemRenderer());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World level, List<String> components, ITooltipFlag isAdvanced) {
        ResourceLocation ammoId = this.getAmmoId(stack);
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(index -> {
            String tooltipKey = index.getTooltipKey();
            if (tooltipKey != null) {
                components.add(Components.translatable(tooltipKey).withStyle(TextFormatting.GRAY).getFormattedText());
            }
        });

        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(ammoId);
        if (packInfoObject != null) {
            components.add(Components.translatable(packInfoObject.getName()).withStyle(TextFormatting.BLUE).withStyle(TextFormatting.ITALIC).getFormattedText());
        }
    }
}
