package com.tacz.guns.client.gui;

import com.tacz.guns.legacy.util.FontUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.legacy.render.PoseStack;
import net.minecraft.client.renderer.GlStateManager;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz.guns.client.gui.components.GunPackList;
import com.tacz.guns.client.gui.components.smith.ResultButton;
import com.tacz.guns.client.gui.components.smith.TypeButton;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import com.tacz.guns.util.RenderDistance;
import com.tacz.guns.legacy.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.gui.ImageButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import com.tacz.guns.legacy.util.gui.AbstractContainerScreen;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import com.tacz.guns.legacy.crafting.RecipeManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import com.tacz.guns.legacy.util.Components;

public class GunSmithTableScreen extends AbstractContainerScreen<GunSmithTableMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private static final ResourceLocation SIDE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table_side.png");

    private final LinkedHashMap<ResourceLocation, TabConfig> recipeKeys = Maps.newLinkedHashMap();
    private final Map<ResourceLocation, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();

    private int typePage;
    private ResourceLocation selectedType = null;
    private List<ResourceLocation> selectedRecipeList = new ArrayList<>();

    private int indexPage;
    private @Nullable GunSmithTableRecipe selectedRecipe;
    private @Nullable Int2IntArrayMap playerIngredientCount;

    private int scale = 70;
    private boolean filterEnabled = false;
    private GunPackList filterList;
    private boolean autoByHandFilterApplied = false;

    public GunSmithTableScreen(GunSmithTableMenu menu, InventoryPlayer inventory, ITextComponent title) {
        super(menu, inventory, title);
        this.imageWidth = 344;
        this.imageHeight = 186;
        this.classifyRecipes();
        this.typePage = 0;
        this.indexPage = 0;
        this.selectedRecipe = this.getSelectedRecipe(selectedRecipeList != null && !this.selectedRecipeList.isEmpty() ? this.selectedRecipeList.get(0) : null);
        this.getPlayerIngredientCount(this.selectedRecipe);
    }

    public static void drawModCenteredString(GuiGraphics gui, FontRenderer font, ITextComponent component, int pX, int pY, int color) {
        String text = component.getFormattedText();
        gui.drawString(font, text, pX - FontUtil.width(font, text) / 2, pY, color, false);
    }

    private void classifyRecipes() {
        this.recipes.clear();
        this.recipeKeys.clear();
        ResourceLocation blockId = menu.getBlockId();
        if (blockId == null) {
            return;
        }
        Map<ResourceLocation, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();
        Map<ResourceLocation, TabConfig> recipeKeys = Maps.newLinkedHashMap();

        TimelessAPI.getCommonBlockIndex(blockId).ifPresent(blockIndex -> {
            var tabs = blockIndex.getData().getTabs();
            if (DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                tabs = TabConfig.DEFAULT_TABS;
            }
            for (TabConfig tab : tabs) {
                recipes.put(tab.id(), Lists.newArrayList());
                recipeKeys.put(tab.id(), tab);
            }
        });

        List<Pair<ResourceLocation, ResourceLocation>> recipeIds = Lists.newArrayList();

        if (Minecraft.getMinecraft().world != null) {
            RecipeManager recipeManager = RecipeManager.of(Minecraft.getMinecraft().world);
            List<GunSmithTableRecipe> recipeList = recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get());
            Set<String> namespaces = filterList != null ? filterList.namespaceList() : null;
            for (GunSmithTableRecipe recipe : recipeList) {
                ResourceLocation id = recipe.getId();
                if (namespaces != null && !namespaces.contains(id.getNamespace())) {
                    continue;
                }
                if (!isSuitableForMainHand(recipe)) {
                    continue;
                }
                if (!isNameMatch(recipe)) {
                    continue;
                }

                ResourceLocation groupName = recipe.getResult().getGroup();
                if (recipeKeys.containsKey(groupName)) {
                    recipeIds.add(Pair.of(groupName, id));
                }
            }
        }

        TimelessAPI.getCommonBlockIndex(menu.getBlockId()).map(blockIndex -> {
            if (menu.getBlockId().equals(DefaultAssets.DEFAULT_BLOCK_ID) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                return null;
            }
            RecipeFilter filter = blockIndex.getFilter();
            if (filter != null) {
                return filter.filter(recipeIds, Pair::value);
            }
            return null;
        }).orElse(recipeIds).forEach(entry -> {
            ResourceLocation groupName = entry.key();
            if (recipeKeys.containsKey(groupName)) {
                recipes.computeIfAbsent(groupName, g -> Lists.newArrayList()).add(entry.value());
            }
        });

        for (var entry : recipes.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                this.recipes.put(entry.getKey(), entry.getValue());
                this.recipeKeys.put(entry.getKey(), recipeKeys.get(entry.getKey()));
            }
        }

        if (!this.recipeKeys.containsKey(selectedType)) {
            selectedType = null;
            selectedRecipeList = null;
            indexPage = 0;
        }

        if (!this.recipeKeys.keySet().isEmpty()) {
            if (selectedType == null) {
                selectedType = this.recipeKeys.keySet().iterator().next();
            }
        }

        if (selectedType != null) {
            selectedRecipeList = this.recipes.get(selectedType);
        }
    }

    private boolean isNameMatch(GunSmithTableRecipe recipe) {
        if (filterList != null && StringUtils.isNotBlank(filterList.getSearchText())) {
            String searchText = filterList.getSearchText().toLowerCase();
            String name = recipe.getResult().getResult().getDisplayName();
            return TextFormatting.getTextWithoutFormattingCodes(name).toLowerCase().contains(searchText);
        }
        return true;
    }

    private boolean isSuitableForMainHand(GunSmithTableRecipe recipe) {
        if (filterList != null && filterList.isByHandSelected()) {
            ItemStack result = recipe.getResult().getResult();

            Minecraft minecraft = Minecraft.getMinecraft();
            ItemStack stack = minecraft.player != null ? minecraft.player.getHeldItemMainhand() : ItemStack.EMPTY;
            if (stack.getItem() instanceof IGun igun) {
                if (result.getItem() instanceof IAmmo iAmmo) {
                    return iAmmo.isAmmoOfGun(stack, result);
                }
                if (result.getItem() instanceof IAttachment) {
                    return igun.allowAttachment(stack, result);
                }
                return false;
            }
            if (stack.getItem() instanceof IAttachment) {
                if (result.getItem() instanceof IGun iGun) {
                    return iGun.allowAttachment(result, stack);
                }
                return false;
            }
            if (stack.getItem() instanceof IAmmo iAmmo) {
                if (result.getItem() instanceof IGun) {
                    return iAmmo.isAmmoOfGun(result, stack);
                }
                return false;
            }
        }
        return true;
    }

    private boolean shouldFilterByMainHand() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ItemStack stack = minecraft.player != null ? minecraft.player.getHeldItemMainhand() : ItemStack.EMPTY;
        Item item = stack.getItem();
        return item instanceof IGun || item instanceof IAttachment || item instanceof IAmmo;
    }

    private void updateSelectedRecipeAfterFiltering() {
        if (selectedRecipeList == null || selectedRecipeList.isEmpty()) {
            this.selectedRecipe = null;
            this.playerIngredientCount = null;
            return;
        }
        boolean selectedRecipeExists = this.selectedRecipe != null && selectedRecipeList.contains(this.selectedRecipe.getId());
        if (!selectedRecipeExists) {
            this.selectedRecipe = this.getSelectedRecipe(selectedRecipeList.get(0));
        }
        this.getPlayerIngredientCount(this.selectedRecipe);
    }

    public void setIndexPage(int indexPage) {
        this.indexPage = indexPage;
    }

    @Nullable
    private GunSmithTableRecipe getSelectedRecipe(ResourceLocation recipeId) {
        if (Minecraft.getMinecraft().world != null) {
            RecipeManager recipeManager = RecipeManager.of(Minecraft.getMinecraft().world);
            return recipeManager.byKey(recipeId).orElse(null);
        }
        return null;
    }

    private void getPlayerIngredientCount(GunSmithTableRecipe recipe) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || recipe == null) {
            return;
        }
        List<GunSmithTableIngredient> ingredients = recipe.getInputs();
        int size = ingredients.size();
        this.playerIngredientCount = new Int2IntArrayMap(size);
        for (int i = 0; i < size; i++) {
            GunSmithTableIngredient ingredient = ingredients.get(i);
            InventoryPlayer inventory = player.inventory;
            int count = 0;
            for (ItemStack stack : inventory.mainInventory) {
                if (!stack.isEmpty() && ingredient.getIngredient().apply(stack)) {
                    count = count + stack.getCount();
                }
            }
            playerIngredientCount.put(i, count);
        }
    }

    public void updateIngredientCount() {
        if (this.selectedRecipe != null) {
            this.getPlayerIngredientCount(selectedRecipe);
        }
        this.init();
    }

    @Override
    public void init() {
        super.init();
        if (this.filterList == null) {
            this.filterList = new GunPackList(this.minecraft, 134, this.imageHeight, topPos, topPos+imageHeight+1, 15, recipes, this);
        }
        if (!this.autoByHandFilterApplied && RenderConfig.AUTO_SELECT_GUN_SMITH_TABLE_FILTER.get()) {
            this.filterList.setByHandSelected(this.shouldFilterByMainHand());
            this.autoByHandFilterApplied = true;
        }
        this.filterList.updateSize(134, this.imageHeight, topPos, topPos+imageHeight+1);
        this.filterList.setLeftPos(leftPos);

        this.classifyRecipes();
        this.updateSelectedRecipeAfterFiltering();
        this.clearWidgets();

        this.addTypePageButtons();
        this.addTypeButtons();
        this.addIndexPageButtons();
        this.addIndexButtons();
        this.addRenderableWidget(new FlatColorButton(leftPos - 10, topPos, 9, 9, Components.literal("F"), b -> {
            this.filterEnabled = !this.filterEnabled;
            this.init();
        }).setTooltips("gui.tacz.gun_smith_table.filter"));
        if(this.filterEnabled) {
            this.addRenderableWidget(this.filterList);
        } else {
            this.addScaleButtons();
            this.addUrlButton();
        }
        this.addCraftButton();
    }

    private void addCraftButton() {
        this.addRenderableWidget(new ImageButton(leftPos + 289, topPos + 162, 48, 18, 138, 164, 18, TEXTURE, b -> {
            if (this.selectedRecipe != null && playerIngredientCount != null) {
                // 检查是否能合成，不能就不发包
                List<GunSmithTableIngredient> inputs = selectedRecipe.getInputs();
                int size = inputs.size();
                for (int i = 0; i < size; i++) {
                    if (i >= playerIngredientCount.size()) {
                        return;
                    }
                    int hasCount = playerIngredientCount.get(i);
                    int needCount = inputs.get(i).getCount();
                    boolean isCreative = Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.isCreative();
                    // 拥有数量小于需求数量，不发包
                    if (hasCount < needCount && !isCreative) {
                        return;
                    }
                }
                NetworkHandler.CHANNEL.sendToServer(new ClientMessageCraft(this.selectedRecipe.getId(), this.menu.windowId));
            }
        }));
    }

    private void addUrlButton() {
        this.addRenderableWidget(new ImageButton(leftPos + 112, topPos + 164, 18, 18, 149, 211, 18, TEXTURE, b -> {
            if (this.selectedRecipe != null) {
                ItemStack output = selectedRecipe.getOutput();
                Item item = output.getItem();
                ResourceLocation id;
                if (item instanceof IGun iGun) {
                    id = iGun.getGunId(output);
                } else if (item instanceof IAttachment iAttachment) {
                    id = iAttachment.getAttachmentId(output);
                } else if (item instanceof IAmmo iAmmo) {
                    id = iAmmo.getAmmoId(output);
                } else {
                    return;
                }

                PackInfo packInfo = ClientAssetsManager.INSTANCE.getPackInfo(id);
                if (packInfo == null) {
                    return;
                }
                String url = packInfo.getUrl();
                if (StringUtils.isNotBlank(url) && minecraft != null) {
                    minecraft.displayGuiScreen(new GuiConfirmOpenLink((yes, id1) -> {
                        if (yes) {
                            openUri(url);
                        }
                        minecraft.displayGuiScreen(this);
                    }, url, 0, false));
                }
            }
        }));
    }

    private void addIndexButtons() {
        if (selectedRecipeList == null || selectedRecipeList.isEmpty()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            int finalIndex = i + indexPage * 6;
            if (finalIndex >= selectedRecipeList.size()) {
                break;
            }
            int yOffset = topPos + 66 + 17 * i;
            ResourceLocation recipeId = selectedRecipeList.get(finalIndex);
            GunSmithTableRecipe recipe = getSelectedRecipe(recipeId);
            if (recipe == null) {
                continue;
            }
            ResultButton button = addRenderableWidget(new ResultButton(leftPos + 144, yOffset, recipe.getOutput(), b -> {
                this.selectedRecipe = recipe;
                this.getPlayerIngredientCount(this.selectedRecipe);
                this.init();
            }));
            if (this.selectedRecipe != null && recipe.getId().equals(this.selectedRecipe.getId())) {
                button.setSelected(true);
            }
        }
    }

    private void addTypeButtons() {
        var list = Arrays.asList(recipeKeys.values().toArray(new TabConfig[0]));
        for (int i = 0; i < 7; i++) {
            int typeIndex = typePage * 7 + i;
            if (typeIndex >= recipes.size()) {
                return;
            }
            TabConfig tabConfig = list.get(typeIndex);
            ResourceLocation type = tabConfig.id();
            int xOffset = leftPos + 157 + 24 * i;

            ItemStack icon = tabConfig.icon();

            TypeButton typeButton = new TypeButton(xOffset, topPos + 2, icon, b -> {
                this.selectedType = type;
                this.selectedRecipeList = recipes.get(type);
                this.indexPage = 0;
                this.selectedRecipe = getSelectedRecipe(this.selectedRecipeList.isEmpty() ? null : this.selectedRecipeList.get(0));
                this.getPlayerIngredientCount(this.selectedRecipe);
                this.init();
            });
            typeButton.setTooltip(tabConfig.getName());
            if (this.selectedType.equals(type)) {
                typeButton.setSelected(true);
            }
            this.addRenderableWidget(typeButton);
        }
    }

    private void addIndexPageButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 143, topPos + 56, 96, 6, 40, 166, 6, TEXTURE, b -> {
            if (this.indexPage > 0) {
                this.indexPage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 143, topPos + 171, 96, 6, 40, 186, 6, TEXTURE, b -> {
            if (selectedRecipeList != null && !selectedRecipeList.isEmpty()) {
                int maxIndexPage = (selectedRecipeList.size() - 1) / 6;
                if (this.indexPage < maxIndexPage) {
                    this.indexPage++;
                    this.init();
                }
            }
        }));
    }

    private void addTypePageButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 136, topPos + 4, 18, 20, 0, 162, 20, TEXTURE, b -> {
            if (this.typePage > 0) {
                this.typePage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 327, topPos + 4, 18, 20, 20, 162, 20, TEXTURE, b -> {
            int maxIndexPage = (recipes.size() - 1) / 7;
            if (this.typePage < maxIndexPage) {
                this.typePage++;
                this.init();
            }
        }));
    }

    private void addScaleButtons() {
        this.addRenderableWidget(new ImageButton(leftPos + 5, topPos + 5, 10, 10, 188, 173, 10, TEXTURE, b -> {
            this.scale = Math.min(this.scale + 20, 200);
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 17, topPos + 5, 10, 10, 200, 173, 10, TEXTURE, b -> {
            this.scale = Math.max(this.scale - 20, 10);
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 29, topPos + 5, 10, 10, 212, 173, 10, TEXTURE, b -> {
            this.scale = 70;
        }));
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pMouseX > leftPos + 143 && pMouseX < leftPos + 143 + 94 && pMouseY > topPos + 66 && pMouseY < topPos + 66 + 85) {
            if (pDelta > 0) {
                this.indexPage = Math.max(0, this.indexPage - 1);
            } else {
                int maxIndexPage = (selectedRecipeList.size() - 1) / 6;
                this.indexPage = Math.min(maxIndexPage, this.indexPage + 1);
            }
            this.init();
            return true;
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawModCenteredString(graphics, font, Components.translatable("gui.tacz.gun_smith_table.preview"), leftPos + 108, topPos + 5, 0x555555);
        if (selectedType != null) {
            var config = recipeKeys.get(selectedType);
            if (config != null) {
                graphics.drawString(font, config.getName(), leftPos + 150, topPos + 32, 0x555555, false);
            }
        }
        graphics.drawString(font, Components.translatable("gui.tacz.gun_smith_table.ingredient"), leftPos + 254, topPos + 50, 0x555555, false);
        drawModCenteredString(graphics, font, Components.translatable("gui.tacz.gun_smith_table.craft"), leftPos + 312, topPos + 167, 0xFFFFFF);
        if (!this.filterEnabled && this.selectedRecipe != null) {
            this.renderLeftModel(graphics, this.selectedRecipe);
            this.renderPackInfo(graphics, this.selectedRecipe);
            graphics.drawString(font, Components.translatable("gui.tacz.gun_smith_table.count", this.selectedRecipe.getResult().getResult().getCount()), leftPos + 254, topPos + 140, 0x555555, false);
        }
        if (selectedRecipeList != null && !selectedRecipeList.isEmpty()) {
            renderIngredient(graphics);
        }

        this.renderables.stream().filter(w -> w instanceof ResultButton)
                .forEach(w -> ((ResultButton) w).renderTooltips(stack -> graphics.renderTooltip(font, stack, mouseX, mouseY)));
    }

    private void renderPackInfo(GuiGraphics gui, GunSmithTableRecipe recipe) {
        ItemStack output = recipe.getOutput();
        Item item = output.getItem();
        ResourceLocation id;
        if (item instanceof IGun iGun) {
            id = iGun.getGunId(output);
        } else if (item instanceof IAttachment iAttachment) {
            id = iAttachment.getAttachmentId(output);
        } else if (item instanceof IAmmo iAmmo) {
            id = iAmmo.getAmmoId(output);
        } else {
            return;
        }

        PackInfo packInfo = ClientAssetsManager.INSTANCE.getPackInfo(id);
        PoseStack poseStack = gui.pose();
        if (packInfo != null) {
            poseStack.pushPose();
            poseStack.scale(0.75f, 0.75f, 1);
            ITextComponent nameText = Components.translatable(packInfo.getName());
            gui.drawString(font, nameText, (int) ((leftPos + 6) / 0.75f), (int) ((topPos + 122) / 0.75f), 0x555555, false);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 1);

            int offsetX = (leftPos + 6) * 2;
            int offsetY = (topPos + 123) * 2;
            int nameWidth = FontUtil.width(font, nameText);
            ITextComponent ver = Components.literal("v" + packInfo.getVersion()).withStyle(TextFormatting.UNDERLINE);
            gui.drawString(font, ver, (int) (offsetX + nameWidth * 0.75f / 0.5f + 5), offsetY, 0x555555, false);
            offsetY += 14;

            String descKey = packInfo.getDescription();
            if (StringUtils.isNoneBlank(descKey)) {
                ITextComponent desc = Components.translatable(descKey);
                List<String> split = FontUtil.split(font, desc, 245);
                for (String charSequence : split) {
                    gui.drawString(font, charSequence, offsetX, offsetY, 0x555555, false);
                    offsetY += font.FONT_HEIGHT;
                }
                offsetY += 3;
            }

            gui.drawString(font, Components.translatable("gui.tacz.gun_smith_table.license")
                            .append(Components.literal(packInfo.getLicense()).withStyle(TextFormatting.DARK_GRAY)),
                    offsetX, offsetY, 0x555555, false);
            offsetY += 12;

            List<String> authors = packInfo.getAuthors();
            if (!authors.isEmpty()) {
                gui.drawString(font, Components.translatable("gui.tacz.gun_smith_table.authors")
                                .append(Components.literal(StringUtils.join(authors, ", ")).withStyle(TextFormatting.DARK_GRAY)),
                        offsetX, offsetY, 0x555555, false);
                offsetY += 12;
            }

            gui.drawString(font, Components.translatable("gui.tacz.gun_smith_table.date")
                            .append(Components.literal(packInfo.getDate()).withStyle(TextFormatting.DARK_GRAY)),
                    offsetX, offsetY, 0x555555, false);

            poseStack.popPose();
        } else {
            ResourceLocation recipeId = recipe.getId();
            gui.drawString(font, Components.translatable("gui.tacz.gun_smith_table.error").withStyle(TextFormatting.DARK_RED), leftPos + 6, topPos + 122, 0xAF0000, false);
            gui.drawString(font, Components.translatable("gui.tacz.gun_smith_table.error.id", recipeId.toString()).withStyle(TextFormatting.DARK_RED), leftPos + 6, topPos + 134, 0xFFFFFF, false);
            PackInfo errorPackInfo = ClientAssetsManager.INSTANCE.getPackInfo(id);
            if (errorPackInfo != null) {
                gui.drawString(font, Components.translatable(errorPackInfo.getName()).withStyle(TextFormatting.DARK_RED), leftPos + 6, topPos + 146, 0xAF0000, false);
            }
        }
    }

    private void renderIngredient(GuiGraphics gui) {
        if (this.selectedRecipe == null) {
            return;
        }
        List<GunSmithTableIngredient> inputs = this.selectedRecipe.getInputs();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                int index = i * 2 + j;
                if (index >= inputs.size()) {
                    return;
                }
                int offsetX = leftPos + 254 + 45 * j;
                int offsetY = topPos + 62 + 17 * i;

                GunSmithTableIngredient smithTableIngredient = inputs.get(index);
                Ingredient ingredient = smithTableIngredient.getIngredient();

                ItemStack[] items = ingredient.getMatchingStacks();
                // 1.12.2: an ore dictionary entry without any item (e.g. copper without another mod) has no icon
                if (items.length > 0) {
                    int itemIndex = ((int) (System.currentTimeMillis() / 1_000)) % items.length;
                    ItemStack item = items[itemIndex];

                    gui.renderFakeItem(item, offsetX, offsetY);
                }

                PoseStack poseStack = gui.pose();
                poseStack.pushPose();

                poseStack.translate(0, 0, 200);
                poseStack.scale(0.5f, 0.5f, 1);
                int count = smithTableIngredient.getCount();
                if (Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.isCreative()){
                    gui.drawString(font, String.format("%d/∞", count), (offsetX + 17) * 2, (offsetY + 10) * 2, 0xFFFFFF, false);
                } else {
                    int hasCount = 0;
                    if (playerIngredientCount != null && index < playerIngredientCount.size()) {
                        hasCount = playerIngredientCount.get(index);
                    }
                    int color = count <= hasCount ? 0xFFFFFF : 0xFF0000;
                    gui.drawString(font, String.format("%d/%d", count, hasCount), (offsetX + 17) * 2, (offsetY + 10) * 2, color, false);
                }


                poseStack.popPose();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void renderLeftModel(GuiGraphics gui, GunSmithTableRecipe recipe) {
        // 先标记一下，渲染高模
        RenderDistance.markGuiRenderTimestamp();

        float rotationPeriod = 8f;
        int xPos = leftPos + 60;
        int yPos = topPos + 50;
        int startX = leftPos + 3;
        int startY = topPos + 16;
        int width = 128;
        int height = 99;
        float rotPitch = 15;

        // 1.12.2: scissor in GUI coordinates through GuiGraphics, the item goes through RenderItem#renderItem(stack, FIXED),
        // which lets the item renderers read the display context from the model
        gui.enableScissor(startX, startY, startX + width, startY + height);

        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        minecraft.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(xPos, yPos, 200);
        GlStateManager.translate(8.0D, 8.0D, 0.0D);
        GlStateManager.scale(1.0F, -1.0F, 1.0F);
        GlStateManager.scale(scale, scale, scale);
        float rot = (System.currentTimeMillis() % (int) (rotationPeriod * 1000)) * (360f / (rotationPeriod * 1000));
        GlStateManager.rotate(rotPitch, 1, 0, 0);
        GlStateManager.rotate(rot, 0, 1, 0);
        RenderSystem.enableDepthTest();
        RenderHelper.enableGUIStandardItemLighting();

        minecraft.getRenderItem().renderItem(recipe.getOutput(), ItemCameraTransforms.TransformType.FIXED);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        minecraft.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

        gui.disableScissor();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics gui, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(gui);
        gui.blit(SIDE, leftPos, topPos, 0, 0, 134, 187);
        gui.blit(TEXTURE, leftPos + 136, topPos + 27, 0, 0, 208, 160);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 1.12.2: {@code Util.getPlatform().openUri(..)}, the same reflection {@code GuiScreen} uses for chat links.
     */
    private static void openUri(String url) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", java.net.URI.class).invoke(instance, new java.net.URI(url));
        } catch (Throwable throwable) {
            GunMod.LOGGER.error("Couldn't open link", throwable);
        }
    }
}
