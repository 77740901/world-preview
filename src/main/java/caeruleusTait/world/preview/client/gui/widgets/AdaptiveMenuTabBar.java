package caeruleusTait.world.preview.client.gui.widgets;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.util.Mth;

/**
 * A {@link MenuTabBar} that sizes each tab button to fit its title text instead of
 * splitting the available width equally (which is hard-capped at 400px in vanilla).
 * <p>
 * Long translations (e.g. German "Zwischenspeicherung") no longer need to scroll.
 * When the measured total width exceeds the screen width, it falls back to an equal
 * distribution so no tab collapses to zero width.
 */
public class AdaptiveMenuTabBar extends MenuTabBar {

    private static final int MARGIN = 28;
    private static final int TEXT_PADDING = 16;
    private static final int MIN_TAB_WIDTH = 40;

    private final Font font;

    public AdaptiveMenuTabBar(
            int x, int y, int width, int height,
            TabManager tabManager,
            ImmutableList<TabButton> tabButtons,
            ImmutableList<Tab> tabs,
            Font font
    ) {
        super(x, y, width, height, tabManager, tabButtons, tabs);
        this.font = font;
    }

    @Override
    public void arrangeElements(int width) {
        this.width = width;
        ImmutableList<TabButton> buttons = this.tabButtons;
        ImmutableList<Tab> tabs = this.tabs;
        int count = buttons.size();

        if (count > 0) {
            int available = Math.max(width - MARGIN, 0);

            int[] widths = new int[count];
            int total = 0;
            for (int i = 0; i < count; i++) {
                int textWidth = font.width(tabs.get(i).getTabTitle());
                int w = Math.max(textWidth + TEXT_PADDING, MIN_TAB_WIDTH);
                w = Mth.roundToward(w, 2);
                widths[i] = w;
                total += w;
            }

            if (total <= available) {
                for (int i = 0; i < count; i++) {
                    buttons.get(i).setWidth(widths[i]);
                }
            } else {
                int tabWidth = Mth.roundToward(available / count, 2);
                for (TabButton button : buttons) {
                    button.setWidth(tabWidth);
                }
            }
        }

        // Replicate TabNavigationBar.arrangeElements: position the layout and let it
        // arrange the (already-sized) tab buttons. We cannot call super.arrangeElements
        // here because MenuTabBar would overwrite our widths with its capped values.
        this.layout.setPosition(this.getX(), this.getY());
        // Make the frame as wide as the screen so the (narrower) tab row is centered
        // horizontally instead of hugging the left edge.
        this.layout.setMinWidth(width);
        this.layout.arrangeElements();
    }
}
