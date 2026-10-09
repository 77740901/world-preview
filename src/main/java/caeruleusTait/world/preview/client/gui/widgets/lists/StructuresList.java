package caeruleusTait.world.preview.client.gui.widgets.lists;

import caeruleusTait.world.preview.client.WorldPreviewClient;
import caeruleusTait.world.preview.client.gui.widgets.ToggleButton;
import caeruleusTait.world.preview.client.translate.ExtraTranslations;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEXTURE;
import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEX_HEIGHT;
import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.BUTTONS_TEX_WIDTH;
import static caeruleusTait.world.preview.client.gui.screens.PreviewContainer.StructureRenderInfo;

public class StructuresList extends BaseObjectSelectionList<StructuresList.StructureEntry> {

    public StructuresList(Minecraft minecraft, int width, int height, int x, int y) {
        super(minecraft, width, height, x, y, 24);
    }

    public StructureEntry createEntry(short id, Identifier Identifier, NativeImage icon, Item item, String name, boolean show, boolean showByDefault) {
        return new StructureEntry(id, Identifier, icon, item, name, show, showByDefault);
    }

    @Override
    public void replaceEntries(Collection<StructureEntry> entryList) {
        super.replaceEntries(entryList);
    }

    public class StructureEntry extends BaseObjectSelectionList.Entry<StructuresList.StructureEntry> implements StructureRenderInfo {
        private final short id;
        private final NativeImage icon;
        private final Item item;
        private final ItemStack itemStack;
        private final DynamicTexture iconTexture;
        private final Identifier iconTextureId;
        private final int iconWidth;
        private final int iconHeight;
        private final Identifier structureId;
        private final String baseName;
        private String name;
        private Tooltip tooltip;
        private final boolean showByDefault;
        private final boolean isPrimaryNamespace;

        private boolean show;
        public final ToggleButton toggleVisible;

        public StructureEntry(short id, Identifier structureId, @NotNull NativeImage icon, @Nullable Item item, String name, boolean show, boolean showByDefault) {
            this.id = id;
            this.item = item;
            this.itemStack = this.item == null ? null : createIconStack(this.item);
            this.icon = icon;
            this.structureId = structureId;
            this.iconTextureId = Identifier.fromNamespaceAndPath("world-preview-unofficial", "structure_list_icon_" + id);
            this.iconTexture = new DynamicTexture(() -> "world-preview-unofficial_structure_list_" + id, this.icon);
            this.iconWidth = this.icon.getWidth();
            this.iconHeight = this.icon.getHeight();
            this.showByDefault = showByDefault;
            this.show = show;
            this.toggleVisible = new ToggleButton(
                    0, 0, 20, 20, /* x, y, width, height */
                    140, 20, 20, 20, /* xTexStart, yTexStart, xDiffTex, yDiffTex */
                    BUTTONS_TEXTURE, BUTTONS_TEX_WIDTH, BUTTONS_TEX_HEIGHT, /* Identifier, textureWidth, textureHeight*/
                    this::toggleVisible
            );

            this.iconTexture.upload();
            Minecraft.getInstance().getTextureManager().register(this.iconTextureId, this.iconTexture);
            this.toggleVisible.selected = show;

            this.isPrimaryNamespace = structureId.getNamespace().equals("minecraft");
            if (Objects.equals(structureId.toString(), name) || name == null) {
                this.baseName = WorldPreviewClient.toTitleCase(structureId.getPath().replace("_", " "));
            } else {
                this.baseName = name;
            }
            this.name = resolveName();
            this.tooltip = buildTooltip();
        }

        /**
         * The overlay translation ("More translations") wins when it is enabled and knows this structure,
         * otherwise the name resolved from the author's data files is kept.
         */
        private String resolveName() {
            final String override = ExtraTranslations.structureUnchecked(structureId.toString());
            return override != null ? override : baseName;
        }

        private Tooltip buildTooltip() {
            String tag = "§5§o" + structureId.getNamespace() + "§r\n§9" + structureId.getPath() + "§r";
            return Tooltip.create(Component.literal(this.name + "\n\n" + tag));
        }

        /**
         * Re-resolve the display name so that toggling the translation setting
         * in the settings screen takes effect without reopening the preview.
         */
        public void refreshName() {
            this.name = resolveName();
            this.tooltip = buildTooltip();
        }

        /**
         * Build the ItemStack used as this structure's icon.
         * <p>
         * Item rendering resolves the model through the {@code minecraft:item_model} data component, and
         * {@code GuiGraphicsExtractor#item} returns early - drawing nothing, without any error - when that
         * component is absent. Stacks created for structures whose icon is declared as an {@code "item"}
         * in {@code structure_icons.json} (stronghold, trial chambers) have been observed to come out
         * without it, which is exactly why those two icons never showed up.
         * <p>
         * Defaulting the component to the item's own registry id restores the vanilla model
         * ({@code assets/minecraft/items/<id>.json}) for items such as the eye of ender and the trial key.
         */
        private static ItemStack createIconStack(Item item) {
            final ItemStack stack = new ItemStack(item, 1);
            if (stack.get(DataComponents.ITEM_MODEL) == null) {
                try {
                    final Holder.Reference<Item> holder = item.builtInRegistryHolder();
                    if (holder.isBound()) {
                        stack.set(DataComponents.ITEM_MODEL, holder.key().identifier());
                    }
                } catch (Throwable ignored) {
                    // Item is not present in the built-in registry; the caller falls back to the texture.
                }
            }
            return stack;
        }

        public void reset() {
            show = showByDefault;
            toggleVisible.selected = show;
        }

        private void toggleVisible(Button btn) {
            show = toggleVisible.selected;
        }

        public void setVisible(boolean show) {
            this.show = show;
        }

        @Override
        public Tooltip tooltip() {
            return tooltip;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.empty();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor extractor, int x, int y, boolean isHovered, float partialTick) {
            final int xMin = getX() + 2;
            final int yMin = getY() + 2;
            final int xMax = xMin + iconWidth;
            final int yMax = yMin + iconHeight;

            // Draw the icon texture first as a reliable base layer. Item icons (stronghold,
            // trial chambers) are drawn on top and cover it when they render; if item rendering
            // silently fails (e.g. another mod interferes with the GUI item atlas pipeline),
            // the texture underneath - which is the item's own sprite for item-based structures -
            // guarantees the slot is never left blank or showing a wrong "unknown" icon.
            extractor.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    iconTextureId,
                    xMin, yMin,
                    0.0F, 0.0F,
                    iconWidth, iconHeight,
                    iconWidth, iconHeight
            );
            if (hasRenderableItem()) {
                extractor.item(itemStack, xMin, yMin);
            }
            String formatName = isPrimaryNamespace ? name : "§o" + name;
            extractor.text(minecraft.font, formatName, getX() + 16 + 4, getY() + 6, -1);
            toggleVisible.setPosition(getRowRight() - 22, getY());
            toggleVisible.extractRenderState(extractor, 0, 0, partialTick);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean playSound) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (toggleVisible.isMouseOver(event.x(), event.y())) {
                toggleVisible.onPress(event);
            }
            return true;
        }

        public String name() {
            return name;
        }

        public boolean showByDefault() {
            return showByDefault;
        }

        public boolean show() {
            return show;
        }

        public short id() {
            return id;
        }

        public Item item() {
            return item;
        }

        public ItemStack itemStack() {
            return itemStack;
        }

        /**
         * Whether this entry can actually be drawn as an item.
         * <p>
         * Modern Minecraft resolves item rendering through the {@code minecraft:item_model} data component,
         * and {@code GuiGraphicsExtractor#item} returns early - drawing nothing at all - when that component
         * is absent. Structures whose icon is defined by an {@code "item"} entry in
         * {@code structure_icons.json} (for example stronghold and trial chambers) depend on this path.
         */
        public boolean hasRenderableItem() {
            return itemStack != null
                    && !itemStack.isEmpty()
                    && itemStack.get(DataComponents.ITEM_MODEL) != null;
        }
    }

}
