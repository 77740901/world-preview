package caeruleusTait.world.preview.client.gui.widgets.lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;

import java.util.Collection;

public abstract class BaseObjectSelectionList<E extends BaseObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
    protected BaseObjectSelectionList(Minecraft minecraft, int width, int height, int x, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }

    @Override
    public int getRowLeft() {
        return getX();
    }

    @Override
    public int getRowRight() {
        return getX() + width - 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - 6;
    }

    @Override
    protected int scrollBarX() {
        return getRowRight();
    }

    @Override
    protected void extractSelection(final GuiGraphicsExtractor graphics, final E entry, final int outlineColor) {
        int outlineX0 = entry.getX();
        int outlineY0 = entry.getY() - 2;
        int outlineX1 = outlineX0 + entry.getWidth();
        int outlineY1 = outlineY0 + entry.getHeight();
        graphics.fill(outlineX0, outlineY0, outlineX1, outlineY1, outlineColor);
        graphics.fill(outlineX0 + 1, outlineY0 + 1, outlineX1 - 1, outlineY1 - 1, -16777216);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);

        E hovered = getHovered();
        if (hovered != null) {
            Tooltip tooltip = hovered.tooltip();
            if (tooltip != null) {
                graphics.setTooltipForNextFrame(minecraft.font, tooltip.toCharSequence(minecraft), mouseX, mouseY);
            }
        }
    }

    /**
     * Make public
     */
    @Override
    public void replaceEntries(Collection<E> entryList) {
        super.replaceEntries(entryList);
    }

    public abstract static class Entry<E extends Entry<E>> extends ObjectSelectionList.Entry<E> {
        @Override
        public int getY() {
            return super.getY() + 2;
        }

        public Tooltip tooltip() {
            return null;
        }
    }
}
