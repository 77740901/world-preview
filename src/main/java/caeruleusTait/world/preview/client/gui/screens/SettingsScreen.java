package caeruleusTait.world.preview.client.gui.screens;

import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.backend.color.PreviewData;
import caeruleusTait.world.preview.backend.color.PreviewMappingData;
import caeruleusTait.world.preview.client.gui.screens.settings.BiomesTab;
import caeruleusTait.world.preview.client.gui.screens.settings.CacheTab;
import caeruleusTait.world.preview.client.gui.screens.settings.DimensionsTab;
import caeruleusTait.world.preview.client.gui.screens.settings.GeneralTab;
import caeruleusTait.world.preview.client.gui.screens.settings.HeightmapTab;
import caeruleusTait.world.preview.client.gui.screens.settings.SamplingTab;
import caeruleusTait.world.preview.client.gui.widgets.AdaptiveMenuTabBar;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.stream.Collectors;

import static caeruleusTait.world.preview.client.WorldPreviewComponents.SETTINGS_TITLE;

public class SettingsScreen extends Screen {
    public static final Identifier HEADER_SEPERATOR = Identifier.parse("textures/gui/header_separator.png");
    public static final Identifier FOOTER_SEPERATOR = Identifier.parse("textures/gui/footer_separator.png");
    public static final Identifier LIGHT_DIRT_BACKGROUND = Identifier.parse("textures/gui/light_dirt_background.png");

    private final Screen lastScreen;
    private final PreviewContainer previewContainer;

    private TabManager tabManager;
    private TabNavigationBar tabNavigationBar;
    private GridLayout bottomButtons;

    public SettingsScreen(Screen lastScreen, PreviewContainer previewContainer) {
        super(SETTINGS_TITLE);
        this.lastScreen = lastScreen;
        this.previewContainer = previewContainer;

        this.tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    }

    @Override
    protected void init() {
        Tab[] tabArray = new Tab[]{
                new GeneralTab(minecraft, previewContainer),
                new CacheTab(minecraft, previewContainer.dataProvider()),
                new SamplingTab(minecraft),
                new HeightmapTab(minecraft, previewContainer.previewData(), WorldPreview.get().biomeColorMap()),
                new DimensionsTab(minecraft, previewContainer.levelStemKeys()),
                new BiomesTab(minecraft, previewContainer)
        };
        ImmutableList<Tab> tabs = ImmutableList.copyOf(tabArray);
        ImmutableList.Builder<TabButton> buttonBuilder = ImmutableList.builder();
        for (Tab tab : tabArray) {
            buttonBuilder.add(new MenuTabBar.MenuTabButton(tabManager, tab, 0, 24));
        }
        tabNavigationBar = new AdaptiveMenuTabBar(
                0, 0, this.width, 24, tabManager, buttonBuilder.build(), tabs, minecraft.font
        );
        tabNavigationBar.selectTab(0, false);
        addRenderableWidget(tabNavigationBar);

        bottomButtons = new GridLayout().columnSpacing(10);
        GridLayout.RowHelper rowHelper = bottomButtons.createRowHelper(1);
        rowHelper.addChild(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).build());
        this.bottomButtons.visitWidgets((abstractWidget) -> {
            abstractWidget.setTabOrderGroup(1);
            this.addRenderableWidget(abstractWidget);
        });

        repositionElements();
    }

    public void reopen() {
        // Rebuild the whole settings screen so every widget recomputes its width for the new language.
        // Vanilla Checkbox caches its width at construction and never re-adjusts when its text changes.
        minecraft.setScreenAndShow(new SettingsScreen(this.lastScreen, this.previewContainer));
    }

    @Override
    public void repositionElements() {
        if (tabNavigationBar != null) {
            tabNavigationBar.setWidth(this.width);
            tabNavigationBar.arrangeElements(this.width);

            bottomButtons.arrangeElements();
            FrameLayout.centerInRectangle(this.bottomButtons, 0, this.height - 36, this.width, 36);
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenRectangle = new ScreenRectangle(0, i, this.width, this.bottomButtons.getY() - i);
            this.tabManager.setTabArea(screenRectangle);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        int y = (int) Mth.roundToward(this.height - 36 - 2, 2);
        AbstractTexture footerTex = minecraft.getTextureManager().getTexture(FOOTER_SEPERATOR);
        GuiGraphicsExtractor.blit(footerTex.getTextureView(), footerTex.getSampler(), 0, y, this.width, 2, 0.0F, 0.0F, (float) this.width / 32.0F, 1.0F);
        super.extractRenderState(GuiGraphicsExtractor, i, j, f);
    }

    @Override
    public void onClose() {
        Map<Identifier, PreviewMappingData.ColorEntry> toWrite = previewContainer.allBiomes()
                .stream()
                .filter(x -> x.dataSource() == PreviewData.DataSource.CONFIG)
                .collect(
                        Collectors.toMap(
                                x -> x.entry().key().identifier(),
                                x -> new PreviewMappingData.ColorEntry(PreviewData.DataSource.CONFIG, x.color(), x.isCave(), x.name())
                        )
                );
        WorldPreview.get().writeUserColorConfig(toWrite);

        // Apply transient changes to the color data
        previewContainer.patchColorData();
        previewContainer.refreshStructureNames();
        previewContainer.resetTabs();

        // Go back
        minecraft.setScreenAndShow(lastScreen);
    }


}
