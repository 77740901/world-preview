package caeruleusTait.world.preview.client.gui.widgets;

import caeruleusTait.world.preview.RenderSettings;
import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.WorldPreviewConfig;
import caeruleusTait.world.preview.backend.WorkManager;
import caeruleusTait.world.preview.backend.color.PreviewData;
import caeruleusTait.world.preview.backend.storage.PreviewSection;
import caeruleusTait.world.preview.backend.storage.PreviewStorage;
import caeruleusTait.world.preview.client.WorldPreviewClient;
import caeruleusTait.world.preview.client.gui.PreviewDisplayDataProvider;
import caeruleusTait.world.preview.client.gui.widgets.lists.BiomesList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.shorts.Short2LongMap;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Queue;

import static caeruleusTait.world.preview.client.WorldPreviewComponents.MSG_ERROR_SETUP_FAILED;
import static caeruleusTait.world.preview.client.WorldPreviewComponents.MSG_PREVIEW_SETUP_LOADING;

public class PreviewDisplay extends AbstractWidget implements AutoCloseable {
    private final Minecraft minecraft;
    private final PreviewDisplayDataProvider dataProvider;
    private final WorkManager workManager;
    private final RenderSettings renderSettings;
    private final WorldPreviewConfig config;
    private Short2LongMap visibleBiomes;
    private Short2LongMap visibleStructures;
    private NativeImage previewImg;
    private DynamicTexture previewTexture;
    private long[] workingVisibleBiomes;
    private long[] workingVisibleStructures;
    private int[] colorMap;
    private int[] colorMapGrayScale;
    private int[] heightColorMap;
    private int[] noiseColorMap;
    private boolean[] cavesMap;
    private IconData[] structureIcons;
    private IconData playerIcon;
    private IconData spawnIcon;
    private ItemStack[] structureItems;
    private PreviewDisplayDataProvider.StructureRenderInfo[] structureRenderInfoMap;
    private final NativeImage dummyIcon;

    private Component coordinatesCopiedMsg = null;
    private Instant coordinatesCopiedTime = null;

    private int texWidth = 100;
    private int texHeight = 100;

    private short selectedBiomeId = -1;
    private boolean highlightCaves;

    private double totalDragX = 0;
    private double totalDragZ = 0;

    private int scaleBlockPos = 1;

    private StructHoverHelperCell[] hoverHelperGrid;
    private final int hoverHelperGridCellSize = 64;
    private int hoverHelperGridWidth;
    private int hoverHelperGridHeight;

    private Queue<Long> frametimes = new ArrayDeque<>();

    private boolean clicked = false;
    private Tooltip pendingTooltip = null;
    private int tooltipRenderX = 0;
    private int tooltipRenderY = 0;

    private record IconData(@NotNull NativeImage img, @NotNull DynamicTexture texture, @NotNull Identifier identifier) {
        public void close() {
            texture.close();
            img.close();
        }
    }

    public PreviewDisplay(Minecraft minecraft, PreviewDisplayDataProvider dataProvider, Component component) {
        super(0, 0, 100, 100, component);
        this.minecraft = minecraft;
        this.workManager = WorldPreview.get().workManager();
        this.dataProvider = dataProvider;
        this.visibleBiomes = new Short2LongOpenHashMap();
        this.visibleStructures = new Short2LongOpenHashMap();
        this.renderSettings = WorldPreview.get().renderSettings();
        this.config = WorldPreview.get().cfg();
        this.dummyIcon = new NativeImage(16, 16, true);
        this.structureIcons = new IconData[0];
        resizeImage();
    }

    public void resizeImage() {
        closeDisplayTextures();
        previewImg = new NativeImage(NativeImage.Format.RGBA, texWidth, texHeight, true);
        previewTexture = new DynamicTexture(() -> "world-preview-unofficial_display", previewImg);
        WorldPreviewClient.registerDisplayTexture(previewTexture);
        scaleBlockPos = (QuartPos.SIZE / renderSettings.quartExpand()) * renderSettings.quartStride();
        hoverHelperGridWidth = (texWidth / hoverHelperGridCellSize) + 1;
        hoverHelperGridHeight = (texHeight / hoverHelperGridCellSize) + 1;
        hoverHelperGrid = new StructHoverHelperCell[hoverHelperGridWidth * hoverHelperGridHeight];
        for (int i = 0; i < hoverHelperGrid.length; ++i) {
            hoverHelperGrid[i] = new StructHoverHelperCell(new ArrayList<>());
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.texWidth = this.width * (int) minecraft.getWindow().getGuiScale();
        this.texHeight = this.height * (int) minecraft.getWindow().getGuiScale();
        resizeImage();
    }

    public void reloadData() {
        // Cleanup previous
        closeIconTextures();

        PreviewData.BiomeData[] rawBiomeMap = dataProvider.previewData().biomeId2BiomeData();
        structureRenderInfoMap = dataProvider.renderStructureMap();
        structureItems = dataProvider.structureItems();
        structureIcons = IntStream.range(0, dataProvider.structureIcons().length)
                .mapToObj(i -> {
                    NativeImage img = dataProvider.structureIcons()[i];
                    Identifier id = Identifier.fromNamespaceAndPath("world-preview-unofficial", "structure_icon_" + i);
                    DynamicTexture tex = new DynamicTexture(() -> "world-preview-unofficial_structure_" + i, img);
                    tex.upload();
                    minecraft.getTextureManager().register(id, tex);
                    return new IconData(img, tex, id);
                })
                .toArray(IconData[]::new);
        playerIcon = new IconData(dataProvider.playerIcon(), new DynamicTexture(() -> "world-preview-unofficial_player", dataProvider.playerIcon()), Identifier.fromNamespaceAndPath("world-preview-unofficial", "player_icon"));
        spawnIcon = new IconData(dataProvider.spawnIcon(), new DynamicTexture(() -> "world-preview-unofficial_spawn", dataProvider.spawnIcon()), Identifier.fromNamespaceAndPath("world-preview-unofficial", "spawn_icon"));
        playerIcon.texture.upload();
        spawnIcon.texture.upload();
        minecraft.getTextureManager().register(playerIcon.identifier, playerIcon.texture);
        minecraft.getTextureManager().register(spawnIcon.identifier, spawnIcon.texture);
        try {
            // ColorMap.getARGB() produces true ARGB, but the texture write path below expects
            // ABGR-packed colors (setPixelABGR / the fillRect channel swap). Convert here, the
            // same way the biome LUT above goes through textureColor().
            final int[] rawHeightLut = dataProvider.heightColorMap();
            for (int i = 0; i < rawHeightLut.length; ++i) {
                rawHeightLut[i] = textureColor(rawHeightLut[i]);
            }
            heightColorMap = rawHeightLut;
            final int[] rawNoiseLut = dataProvider.noiseColorMap();
            for (int i = 0; i < rawNoiseLut.length; ++i) {
                rawNoiseLut[i] = textureColor(rawNoiseLut[i]);
            }
            noiseColorMap = rawNoiseLut;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        workingVisibleBiomes = new long[rawBiomeMap.length];
        workingVisibleStructures = new long[structureIcons.length];
        colorMap = new int[rawBiomeMap.length];
        colorMapGrayScale = new int[rawBiomeMap.length];
        cavesMap = new boolean[rawBiomeMap.length];
        for (short i = 0; i < rawBiomeMap.length; ++i) {
            colorMap[i] = textureColor(rawBiomeMap[i].color());
            colorMapGrayScale[i] = grayScale(colorMap[i]);
            cavesMap[i] = rawBiomeMap[i].isCave();
        }
    }

    private void closeIconTextures() {
        if (structureIcons != null) {
            Arrays.stream(structureIcons).forEach(IconData::close);
        }
        if (playerIcon != null) {
            playerIcon.texture.close();
        }
        if (spawnIcon != null) {
            spawnIcon.texture.close();
        }
    }

    private void closeDisplayTextures() {
        if (previewTexture != null) {
            previewTexture.close();
        }
        if (previewImg != null) {
            previewImg.close();
        }
    }

    public void close() {
        closeIconTextures();
        closeDisplayTextures();
    }

    public BlockPos center() {
        if (totalDragX == 0 && totalDragZ == 0) {
            return renderSettings.center();
        }
        return new BlockPos(
                (int) (renderSettings.center().getX() + totalDragX),
                renderSettings.center().getY(),
                (int) (renderSettings.center().getZ() + totalDragZ)
        );
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        final int colorBorder = 0xFF666666;

        final int xMin = getX();
        final int yMin = getY();
        final int xMax = xMin + width;
        final int yMax = yMin + height;

        final Instant renderStart = Instant.now();
        queueGeneration();
        synchronized (dataProvider) {
            if (dataProvider.setupFailed()) {
                previewImg.fillRect(0, 0, texWidth, texHeight, 0xFF000000);
                previewTexture.upload();
                WorldPreviewClient.renderTexture(extractor, xMin, yMin, xMax, yMax, texWidth, texHeight);

                final List<MutableComponent> lines = MSG_ERROR_SETUP_FAILED.getString().lines().map(Component::literal).toList();

                final int centerX = getX() + (width / 2);
                final int centerY = getY() + (height / 2) - ((lines.size() / 2) * (minecraft.font.lineHeight + 4));

                for (int i = 0; i < lines.size(); ++i) {
                    final Component line = lines.get(i);
                    final int offsetY = i * (minecraft.font.lineHeight + 4);
                    extractor.centeredText(minecraft.font, line, centerX, centerY + offsetY, -1);
                }
            } else if (dataProvider.isUpdating()) {
                previewImg.fillRect(0, 0, texWidth, texHeight, 0xFF000000);
                previewTexture.upload();
                WorldPreviewClient.renderTexture(extractor, xMin, yMin, xMax, yMax, texWidth, texHeight);

                final int centerX = getX() + (width / 2);
                final int centerY = getY() + (height / 2);
                extractor.centeredText(minecraft.font, MSG_PREVIEW_SETUP_LOADING, centerX, centerY, -1);
            } else {
                Arrays.fill(workingVisibleBiomes, (short) 0);
                Arrays.fill(workingVisibleStructures, (short) 0);
                Arrays.stream(hoverHelperGrid).forEach(cell -> cell.entries.clear());
                final List<RenderHelper> renderData = generateRenderData();
                updateTexture(renderData);

                previewTexture.upload();

                // Render the main texture
                WorldPreviewClient.renderTexture(extractor, xMin, yMin, xMax, yMax, texWidth, texHeight);

                // Overlay structure icons
                // Clip icons to the map area so partially visible icons at the edges do not spill
                // over the border (matches 1.21 behavior which used enableScissor here).
                extractor.enableScissor(xMin, yMin, xMax, yMax);
                renderStructures(renderData, extractor);
                renderPlayerAndSpawn(extractor);
                extractor.disableScissor();

                // Update hover info
                biomesChanged();
                updateTooltip((double) mouseX, (double) mouseY);
                tooltipRenderX = mouseX;
                tooltipRenderY = mouseY;
            }
        }

        // Create a border
        // Use a dedicated pipeline that sorts strictly above the GUI_TEXTURED icon draws
        RenderPipeline borderPipeline = WorldPreviewClient.borderPipeline();
        extractor.fill(borderPipeline, xMin-1, yMin-1, xMax+1, yMin, colorBorder);
        extractor.fill(borderPipeline, xMax, yMin, xMax+1, yMax, colorBorder);
        extractor.fill(borderPipeline, xMin-1, yMax, xMax+1, yMax+1, colorBorder);
        extractor.fill(borderPipeline, xMin-1, yMin, xMin, yMax, colorBorder);

        // Render tooltip at mouse position
        // DefaultTooltipPositioner adds +12, -12 offset internally
        if (pendingTooltip != null) {
            var lines = pendingTooltip.toCharSequence(minecraft);
            extractor.setTooltipForNextFrame(minecraft.font, lines, tooltipRenderX, tooltipRenderY);
            pendingTooltip = null;
        }

        // Render copied message
        if (coordinatesCopiedMsg != null) {
            extractor.fill(xMin, yMax - 38, xMax, yMax - 19, 0xAA000000);
            extractor.centeredText(minecraft.font, coordinatesCopiedMsg, xMin + ((xMax - xMin) / 2), yMax - 32, -1);
            if (Duration.between(coordinatesCopiedTime, Instant.now()).toSeconds() >= 8) {
                coordinatesCopiedMsg = null;
                coordinatesCopiedTime = null;
            }
        }

        final Instant renderEnd = Instant.now();
        frametimes.add(Duration.between(renderStart, renderEnd).abs().toMillis());
        while (frametimes.size() > 30) {
            frametimes.poll();
        }
        long sum = frametimes.stream().reduce(0L, Long::sum);

        if (config.showFrameTime) {
            extractor.text(minecraft.font, sum / frametimes.size() + " ms", 5, 5, -1);
        }

    }

    private record TextureCoordinate(int x, int z) {}

    private TextureCoordinate blockToTexture(BlockPos blockPos) {
        BlockPos center = center();
        final int xMin = center.getX() - (texWidth * scaleBlockPos / 2) - 1;
        final int zMin = center.getZ() - (texHeight * scaleBlockPos / 2) - 1;

        // Snap the window to the quart-grid block boundary. generateRenderData() anchors the map
        // texture at the same grid (quart of xMin -> texX 0), so icons move in exact texel sync
        // with the terrain. Without this, block-window vs quart-grid anchors jitter against each
        // other by up to quartExpand-1 texels while dragging.
        final int xMinQuartBlock = QuartPos.fromBlock(xMin) * QuartPos.SIZE;
        final int zMinQuartBlock = QuartPos.fromBlock(zMin) * QuartPos.SIZE;

        return new TextureCoordinate(
                (((blockPos.getX() - xMinQuartBlock) / QuartPos.SIZE) * QuartPos.SIZE) / scaleBlockPos,
                (((blockPos.getZ() - zMinQuartBlock) / QuartPos.SIZE) * QuartPos.SIZE) / scaleBlockPos
        );
    }

    private void putHoverStructEntry(TextureCoordinate pos, StructHoverHelperEntry entry) {
        int cellX = Math.max(0, Math.min(hoverHelperGridWidth - 1, pos.x / hoverHelperGridCellSize));
        int cellZ = Math.max(0, Math.min(hoverHelperGridHeight - 1, pos.z / hoverHelperGridCellSize));
        hoverHelperGrid[(cellX * hoverHelperGridHeight) + cellZ].entries.add(entry);
    }

    private void queueGeneration() {
        final BlockPos center = center();
        final int xMin = center.getX() - (texWidth * scaleBlockPos / 2) - 1;
        final int xMax = center.getX() + (texWidth * scaleBlockPos / 2) + 1;
        final int zMin = center.getZ() - (texHeight * scaleBlockPos / 2) - 1;
        final int zMax = center.getZ() + (texHeight * scaleBlockPos / 2) + 1;
        final int y = center.getY();
        workManager.queueRange(new BlockPos(xMin, y, zMin), new BlockPos(xMax, y, zMax));
    }

    private record RenderHelper(
            PreviewSection dataSection,
            PreviewSection structureSection,
            PreviewSection.AccessData accessData,
            int sectionStartTexX,
            int sectionStartTexZ
    ) {
    }

    private List<RenderHelper> generateRenderData() {
        final BlockPos center = center();
        final int xMin = center.getX() - (texWidth * scaleBlockPos / 2) - 1;
        final int zMin = center.getZ() - (texHeight * scaleBlockPos / 2) - 1;

        final int quartExpand = renderSettings.quartExpand();
        final int quartStride = renderSettings.quartStride();

        final int quartsInWidth = (texWidth / quartExpand) * quartStride;
        final int quartsInHeight = (texHeight / quartExpand) * quartStride;

        final int minQuartX = QuartPos.fromBlock(xMin);
        final int minQuartZ = QuartPos.fromBlock(zMin);

        final int maxQuartX = minQuartX + quartsInWidth;
        final int maxQuartZ = minQuartZ + quartsInHeight;

        int quartX = minQuartX;
        int quartY = QuartPos.fromBlock(center.getY());
        int quartZ = minQuartZ;

        int sectionStartTexX = 0;
        int sectionStartTexZ = 0;

        final List<RenderHelper> res = new ArrayList<>(((quartsInWidth / PreviewSection.SIZE) + 2) * ((quartsInHeight / PreviewSection.SIZE) + 2));

        PreviewStorage storage = workManager.previewStorage();
        if (storage == null) {
            return res;
        }

        // Load sections
        synchronized (storage) {
            while (true) {
                long flag = renderSettings.mode.flag;
                int useY = renderSettings.mode.useY ? quartY : 0;
                PreviewSection dataSection = storage.section4(quartX, useY, quartZ, flag);
                PreviewSection structureSection = storage.section4(quartX, 0, quartZ, PreviewStorage.FLAG_STRUCT_START);
                PreviewSection.AccessData accessData = dataSection.calcQuartOffsetData(quartX, quartZ, maxQuartX, maxQuartZ);

                res.add(new RenderHelper(dataSection, structureSection, accessData, sectionStartTexX, sectionStartTexZ));

                // Can we fit more stuff in the X direction?
                if (accessData.continueX()) {
                    int quartDiffX = accessData.maxX() - accessData.minX();
                    quartX += quartDiffX;
                    sectionStartTexX += (quartDiffX * quartExpand) / quartStride;
                    continue;
                }

                // We are at the end in the X direction, can we continue in the Z direction?
                if (accessData.continueZ()) {
                    int quartDiffZ = accessData.maxZ() - accessData.minZ();
                    quartX = minQuartX;
                    quartZ += quartDiffZ;
                    sectionStartTexZ += (quartDiffZ * quartExpand) / quartStride;
                    sectionStartTexX = 0;
                    continue;
                }

                // We are done drawing now
                break;
            }
        }

        return res;
    }

    private void updateTexture(List<RenderHelper> renderData) {
        int texX = 0;
        int texZ = 0;

        final int quartExpand = renderSettings.quartExpand();
        final int quartStride = renderSettings.quartStride();

        // Render the biomes / heightmap
        for (RenderHelper r : renderData) {
            // Reset icon coords to the current section
            texX = r.sectionStartTexX;

            // Draw all the relevant data in the section
            for(int x = r.accessData.minX(); x < r.accessData.maxX(); x += quartStride) {
                texZ = r.sectionStartTexZ;
                for (int z = r.accessData.minZ(); z < r.accessData.maxZ(); z += quartStride) {

                    // Read the biome data
                    short rawData = r.dataSection.get(x, z);
                    int color = 0xFF000000;
                    switch (renderSettings.mode) {
                        case BIOMES -> {
                            if (rawData >= 0) {
                                color = selectedBiomeId >= 0 || highlightCaves ? colorMapGrayScale[rawData] : colorMap[rawData];
                                if (selectedBiomeId == rawData || (highlightCaves && cavesMap[rawData])) {
                                    color = colorMap[rawData];
                                }
                                workingVisibleBiomes[rawData] += 1;
                            }
                        }
                        case HEIGHTMAP -> {
                            if (rawData > Short.MIN_VALUE) {
                                color = heightColorMap[rawData - dataProvider.yMin()];
                            }
                        }
                        case INTERSECTIONS -> {
                            if (rawData >= 0) {
                                // Main y-intersection
                                color = MapColor.byId(rawData).col;
                                color = textureColor(color == 0 ? 0xFFFFFF : color);
                            } else if(rawData > Short.MIN_VALUE) {
                                // See through one layer of air
                                color = MapColor.byId(-rawData).col;
                                color = highlightColor(textureColor(color == 0 ? 0xFFFFFF : color));
                            }
                        }
                        case NOISE_TEMPERATURE, NOISE_HUMIDITY, NOISE_CONTINENTALNESS, NOISE_EROSION, NOISE_DEPTH, NOISE_WEIRDNESS -> {
                            if (rawData > Short.MIN_VALUE) {
                                final float data = ((float) rawData) / ((float) Short.MAX_VALUE);
                                final int idx = Math.min(1023, Math.max(0, 512 + (int) (data * 512)));
                                color = noiseColorMap[idx];
                            }
                        }
                        case NOISE_PEAKS_AND_VALLEYS -> {
                            if (rawData > Short.MIN_VALUE) {
                                final float data = ((float) rawData) / 0.75f / ((float) Short.MAX_VALUE);
                                final float pvData = NoiseRouterData.peaksAndValleys(Math.min(1.0f, Math.max(-1.0f, data)));
                                final int idx = Math.min(1023, Math.max(0, 512 + (int) (pvData * 512)));
                                color = noiseColorMap[idx];
                            }
                        }
                    }

                    // Draw
                    if (quartExpand > 1) {
                        // fillRect uses setPixelRGBA internally which expects ARGB format,
                        // but our color is in ABGR format from textureColor(). Swap R and B channels.
                        int argbColor = (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
                        previewImg.fillRect(
                                texX,
                                texZ,
                                Math.min(texWidth - texX, quartExpand),
                                Math.min(texHeight - texZ, quartExpand),
                                argbColor
                        );
                    } else {
                        previewImg.setPixelABGR(texX, texZ, color);
                    }

                    texZ += quartExpand;
                }
                texX += quartExpand;
            }
        }
    }

    private void renderStructures(List<RenderHelper> renderData, GuiGraphicsExtractor extractor) {
        if (!config.sampleStructures) {
            return;
        }

        final double guiScale = minecraft.getWindow().getGuiScale();

        // Draw structures
        for (RenderHelper r : renderData) {
            for (PreviewSection.PreviewStruct structure : r.structureSection.structures()) {
                short id = structure.structureId();
                TextureCoordinate texCenter = blockToTexture(structure.center());
                IconData iconData = structureIcons[id];
                NativeImage icon = iconData.img;
                DynamicTexture iconTexture = iconData.texture;
                ItemStack item = structureItems[id];
                // Item rendering goes through the "minecraft:item_model" data component; without it
                // GuiGraphicsExtractor#item draws nothing, so only treat the stack as drawable when
                // the component is present (structures without an item fall back to their texture).
                final boolean drawableItem = item != null
                        && !item.isEmpty()
                        && item.get(DataComponents.ITEM_MODEL) != null;
                if (icon == null && !drawableItem) {
                    continue;
                }
                if (icon == null) {
                    icon = dummyIcon;
                }

                // Check if visible (use texture pixel coordinates)
                final int xMin = -(icon.getWidth() / 2);
                final int xMax = (icon.getWidth() / 2) + 1 + texWidth;
                final int zMin = -(icon.getHeight() / 2);
                final int zMax = (icon.getHeight() / 2) + 1 + texHeight;
                if (texCenter.x < xMin || texCenter.z < zMin || texCenter.x > xMax || texCenter.z > zMax) {
                    continue;
                }

                workingVisibleStructures[id] += 1;

                // Do not render hidden structures, but still count them
                if (!structureRenderInfoMap[id].show() || renderSettings.hideAllStructures) {
                    continue;
                }

                // Convert texture-pixel coordinates to GUI-scaled coordinates
                // texCenter is in texture pixels (0..texWidth). The texture is displayed in widget area
                // (xMin..xMax in GUI coords). Scale factor = (xMax-xMin)/texWidth = 1.0/guiScale per texel.
                // But getX()/getY() is already in GUI coords, and texStartX is in texture pixels.
                // Position = getX() + texStartX / guiScale (since texWidth = width * guiScale)
                final int texStartX = texCenter.x - (icon.getWidth() / 2);
                final int texStartZ = texCenter.z - (icon.getHeight() / 2);

                // Push a scale to render in screen pixel space (matching 1.21 behavior)
                // Items render at 16x16 pixels instead of 16x16 GUI units
                var pose = extractor.pose();
                pose.pushMatrix();
                float pixelScale = (float)(1.0 / guiScale);
                pose.scale(pixelScale, pixelScale);

                final int rXMin = (int) (getX() * guiScale + texStartX);
                final int rZMin = (int) (getY() * guiScale + texStartZ);

                // Draw the icon texture first as a reliable base layer, then the item on top.
                // For item-based structures (stronghold, trial chambers) the base texture is the
                // item's own sprite, so even if item rendering fails the correct icon still shows.
                if (iconTexture != null) {
                    extractor.blit(
                            RenderPipelines.GUI_TEXTURED,
                            iconData.identifier,
                            rXMin, rZMin,
                            0.0F, 0.0F,
                            icon.getWidth(), icon.getHeight(),
                            icon.getWidth(), icon.getHeight()
                    );
                }
                if (drawableItem) {
                    extractor.item(item, rXMin, rZMin);
                }

                pose.popMatrix();

                putHoverStructEntry(
                        texCenter,
                        new StructHoverHelperEntry(
                                new BoundingBox(texStartX, 0, texStartZ, texStartX + icon.getWidth(), 0, texStartZ + icon.getHeight()),
                                structure
                        )
                );
            }
        }
    }

    private void renderPlayerAndSpawn(GuiGraphicsExtractor extractor) {
        if (!config.showPlayer) {
            return;
        }

        PreviewDisplayDataProvider.PlayerData playerData = dataProvider.getPlayerData(minecraft.getUser().getProfileId());
        if (playerData.currentPos() != null) {
            renderStickyIcon(extractor, playerIcon, playerData.currentPos());
        }
        if (playerData.spawnPos() != null) {
            renderStickyIcon(extractor, spawnIcon, playerData.spawnPos());
        }
    }

    /**
     * Render the player and spawn icons in double the size
     */
    private void renderStickyIcon(GuiGraphicsExtractor extractor, IconData iconData, BlockPos pos) {
        final double guiScale = minecraft.getWindow().getGuiScale();
        final NativeImage icon = iconData.img;

        TextureCoordinate texCenter = blockToTexture(pos);
        texCenter = new TextureCoordinate(
                Math.max(0, Math.min(texWidth, texCenter.x)),
                Math.max(0, Math.min(texHeight, texCenter.z))
        );

        // Render icon at pixel coordinates (double size for player/spawn)
        final int texStartX = texCenter.x - icon.getWidth();
        final int texStartZ = texCenter.z - icon.getHeight();

        var pose = extractor.pose();
        pose.pushMatrix();
        float pixelScale = (float)(1.0 / guiScale);
        pose.scale(pixelScale * 2, pixelScale * 2);

        final int rXMin = (int) ((getX() * guiScale + texStartX) / 2.0);
        final int rZMin = (int) ((getY() * guiScale + texStartZ) / 2.0);

        extractor.blit(
                RenderPipelines.GUI_TEXTURED,
                iconData.identifier,
                rXMin, rZMin,
                0.0F, 0.0F,
                icon.getWidth(), icon.getHeight(),
                icon.getWidth(), icon.getHeight()
        );

        pose.popMatrix();
    }

    private void biomesChanged() {
        Short2LongMap tempBiomesSet = new Short2LongOpenHashMap(workingVisibleBiomes.length);
        Short2LongMap tempStructuresSet = new Short2LongOpenHashMap(workingVisibleStructures.length);
        for (short i = 0; i < workingVisibleBiomes.length; ++i) {
            if (workingVisibleBiomes[i] > 0) {
                tempBiomesSet.put(i, workingVisibleBiomes[i]);
            }
        }
        for (short i = 0; i < workingVisibleStructures.length; ++i) {
            if (workingVisibleStructures[i] > 0) {
                tempStructuresSet.put(i, workingVisibleStructures[i]);
            }
        }

        if (!tempBiomesSet.equals(visibleBiomes)) {
            dataProvider.onVisibleBiomesChanged(tempBiomesSet);
        }
        if (!tempStructuresSet.equals(visibleStructures)) {
            dataProvider.onVisibleStructuresChanged(tempStructuresSet);
        }
        visibleBiomes = tempBiomesSet;
        visibleStructures = tempStructuresSet;
    }

    private HoverInfo hoveredBiome(double mouseX, double mouseY) {
        if (!isHovered || workManager.previewStorage() == null) {
            return null;
        }
        int guiScale = (int) minecraft.getWindow().getGuiScale();

        final BlockPos center = center();
        final int xMin = center.getX() - (texWidth * scaleBlockPos / 2) - 1;
        final int zMin = center.getZ() - (texHeight * scaleBlockPos / 2) - 1;

        final int xPos = (int) ((mouseX - getX()) * guiScale * scaleBlockPos);
        final int zPos = (int) ((mouseY - getY()) * guiScale * scaleBlockPos);

        int quartX = QuartPos.fromBlock(xMin + xPos);
        int quartY = QuartPos.fromBlock(center.getY());
        int quartZ = QuartPos.fromBlock(zMin + zPos);
        short biome = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_BIOME);
        short height = workManager.previewStorage().getRawData4(quartX, 0, quartZ, PreviewStorage.FLAG_HEIGHT);

        if (biome < 0) {
            return new HoverInfo(
                    xMin + xPos, center.getY(), zMin + zPos, null, height,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN
            );
        }

        final short temperature = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_TEMPERATURE);
        final short humidity = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_HUMIDITY);
        final short continentalness = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_CONTINENTALNESS);
        final short erosion = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_EROSION);
        final short depth = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_DEPTH);
        final short weirdness = workManager.previewStorage().getRawData4(quartX, quartY, quartZ, PreviewStorage.FLAG_NOISE_WEIRDNESS);

        if (temperature == Short.MIN_VALUE && humidity == Short.MIN_VALUE && continentalness == Short.MIN_VALUE && erosion == Short.MIN_VALUE && depth == Short.MIN_VALUE && weirdness == Short.MIN_VALUE) {
            return new HoverInfo(
                    xMin + xPos, center.getY(), zMin + zPos, dataProvider.biome4Id(biome), height,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN
            );      
        } else {
            return new HoverInfo(
                    xMin + xPos, center.getY(), zMin + zPos, dataProvider.biome4Id(biome), height,
                    temperature / 1.0 / Short.MAX_VALUE,
                    humidity / 1.0 / Short.MAX_VALUE,
                    continentalness / 0.5 / Short.MAX_VALUE,
                    erosion / 1.0 / Short.MAX_VALUE,
                    depth / 0.5 / Short.MAX_VALUE,
                    weirdness / 0.75 / Short.MAX_VALUE,
                    NoiseRouterData.peaksAndValleys(Math.min(1.0f, Math.max(-1.0f, weirdness / 0.75f / Short.MAX_VALUE)))
            );
        }
    }

    private List<StructHoverHelperEntry> hoveredStructures(double mouseX, double mouseY) {
        if (!isHovered) {
            return List.of();
        }

        int guiScale = (int) minecraft.getWindow().getGuiScale();
        final int xTexPos = (int) ((mouseX - getX()) * guiScale);
        final int zTexPos = (int) ((mouseY - getY()) * guiScale);

        final int xGridPos = xTexPos / hoverHelperGridCellSize;
        final int zGridPos = zTexPos / hoverHelperGridCellSize;

        final List<StructHoverHelperEntry> res = new ArrayList<>();
        for (int x = xGridPos - 1; x <= xGridPos + 1; ++x) {
            for (int z = zGridPos - 1; z <= zGridPos + 1; ++z) {
                if (x < 0 || x >= hoverHelperGridWidth || z < 0 || z >= hoverHelperGridHeight) {
                    continue;
                }
                StructHoverHelperCell cell = hoverHelperGrid[(x * hoverHelperGridHeight) + z];
                for (var entry : cell.entries) {
                    if (entry.boundingBox.isInside(xTexPos, 0, zTexPos)) {
                        res.add(entry);
                    }
                }
            }
        }
        return res;
    }

    private static String nameFormatter(String s) {
        int idx = s.indexOf(':');
        if (idx < 0) {
            return "§e" + s + "§r";
        }
        return String.format("§5§o%s§r§5:%s§r", s.substring(0, idx), s.substring(idx + 1));
    }

    private void setTooltipNow(Tooltip tooltip) {
        this.pendingTooltip = tooltip;
    }

    private void updateTooltip(double mouseX, double mouseY) {
        HoverInfo hoverInfo = hoveredBiome(mouseX, mouseY);
        List<StructHoverHelperEntry> structuresInfos = hoveredStructures(mouseX, mouseY);
        if (hoverInfo == null && structuresInfos.isEmpty()) {
            return;
        }

        String blockPosTemplate = "§3X=§b%d§r §3Y=§b%d§r §3Z=§b%d§r";


        if (!structuresInfos.isEmpty()) {
            var structure = structuresInfos.get(0).structure;
            if (config.showControls) {
                setTooltipNow(Tooltip.create(Component.translatable(
                        "world-preview-unofficial.preview-display.struct.tooltip.controls",
                        nameFormatter(dataProvider.structure4Id(structure.structureId()).name()),
                        blockPosTemplate.formatted(structure.center().getX(), structure.center().getY(), structure.center().getZ())
                )));
            } else {
                setTooltipNow(Tooltip.create(Component.translatable(
                        "world-preview-unofficial.preview-display.struct.tooltip",
                        nameFormatter(dataProvider.structure4Id(structure.structureId()).name()),
                        blockPosTemplate.formatted(structure.center().getX(), structure.center().getY(), structure.center().getZ())
                )));
            }
            return;
        }

        String height = hoverInfo.height > Short.MIN_VALUE ? String.format("§b%d§r", hoverInfo.height) : "§7<N/A>§r";
        String noise = "";
        if (!Double.isNaN(hoverInfo.temperature)) {
            noise = "\n\n§3T=§b%.2f§r §3H=§b%.2f§r §3C=§b%.2f§r\n§3E=§b%.2f§r §3D=§b%.2f§r §3W=§b%.2f§r\n§3PV=§b%.2f§r".formatted(
                    hoverInfo.temperature,
                    hoverInfo.humidity,
                    hoverInfo.continentalness,
                    hoverInfo.erosion,
                    hoverInfo.depth,
                    hoverInfo.weirdness,
                    hoverInfo.pv
            );
        }

        if (config.showControls) {
            setTooltipNow(Tooltip.create(Component.translatable(
                    "world-preview-unofficial.preview-display.tooltip.controls",
                    nameFormatter(hoverInfo.entry == null ? "<N/A>" : hoverInfo.entry.name()),
                    blockPosTemplate.formatted(hoverInfo.blockX, hoverInfo.blockY, hoverInfo.blockZ),
                    height,
                    noise
            )));
        } else {
            setTooltipNow(Tooltip.create(Component.translatable(
                    "world-preview-unofficial.preview-display.tooltip",
                    nameFormatter(hoverInfo.entry == null ? "<N/A>" : hoverInfo.entry.name()),
                    blockPosTemplate.formatted(hoverInfo.blockX, hoverInfo.blockY, hoverInfo.blockZ),
                    height,
                    noise
                    )));
        }
    }

    @Override
    public void playDownSound(SoundManager handler) {
        // By default, do nothing
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean playSound) {
        clicked = true;
    }

    @Override
    protected void onDrag(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        final double guiScale = minecraft.getWindow().getGuiScale();
        totalDragX -= (dragX * guiScale) * ((double) scaleBlockPos);
        totalDragZ -= (dragY * guiScale) * ((double) scaleBlockPos);
    }

    @Override
    public void onRelease(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();

        if(clicked == false) {
            return;
        }
        clicked = false;

        if (Math.abs(totalDragX) <= 4 && Math.abs(totalDragZ) <= 4) {
            HoverInfo hoverInfo = hoveredBiome(mouseX, mouseY);
            if (hoverInfo == null || hoverInfo.entry == null) {
                return;
            }

            super.playDownSound(minecraft.getSoundManager());
            if (selectedBiomeId == hoverInfo.entry.id()) {
                dataProvider.onBiomeVisuallySelected(null);
            } else {
                dataProvider.onBiomeVisuallySelected(hoverInfo.entry);
            }
        }

        renderSettings.setCenter(center());

        totalDragX = 0;
        totalDragZ = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        synchronized (dataProvider) {
            if (dataProvider.isUpdating()) {
                return true;
            }
            if ((deltaX + deltaY) > 0.0) {
                renderSettings.decrementY();
            } else if ((deltaX + deltaY) < 0.0) {
                renderSettings.incrementY();
            }
            return true;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean playSound) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();
        // Copy TP command to clipboard on right click
        if (this.isMouseOver(mouseX, mouseY) && button == 1) {
            this.playDownSound(minecraft.getSoundManager());

            final HoverInfo hoverInfo = hoveredBiome(mouseX, mouseY);
            if (hoverInfo == null) {
                return true;
            }
            final String coordinates = String.format(
                    "%s %s %s",
                    hoverInfo.blockX,
                    hoverInfo.height == Short.MIN_VALUE ? "~" : hoverInfo.height,
                    hoverInfo.blockZ
            );

            minecraft.keyboardHandler.setClipboard(coordinates);
            coordinatesCopiedTime = Instant.now();
            coordinatesCopiedMsg = Component.translatable("world-preview-unofficial.preview-display.coordinates.copied", coordinates);
            return true;
        }

        return super.mouseClicked(event, playSound);
    }

    private static int textureColor(int orig) {
        final int R = (orig >> 16) & 0xFF;
        final int G = (orig >> 8) & 0xFF;
        final int B = (orig >> 0) & 0xFF;
        return (R << 0) | (G << 8) | (B << 16) | (0xFF << 24);
    }

    private static int highlightColor(int orig) {
        int R = (orig >> 16) & 0xFF;
        int G = (orig >> 8) & 0xFF;
        int B = (orig >> 0) & 0xFF;

        final int diff = ((R + G + B) / 3) > 200 ? -100 : 100;

        R += diff;
        G += diff;
        B += diff;
        R = Math.max(Math.min(R, 255), 0);
        G = Math.max(Math.min(G, 255), 0);
        B = Math.max(Math.min(B, 255), 0);
        return (0xFF << 24) | (R << 16) | (G << 8) | B;
    }

    private static int grayScale(int orig) {
        int R = (orig >> 16) & 0xFF;
        int G = (orig >> 8) & 0xFF;
        int B = (orig >> 0) & 0xFF;

        final int gray = Math.max(32, Math.min(256 - 32, (R + G + B) / 3));
        return (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
    }

    /**
     * Negative values for none
     */
    public void setSelectedBiomeId(short biomeId) {
        selectedBiomeId = biomeId;
    }

    public void setHighlightCaves(boolean highlightCaves) {
        this.highlightCaves = highlightCaves;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Nothing to do
    }

    private record HoverInfo(
            int blockX,
            int blockY,
            int blockZ,
            BiomesList.BiomeEntry entry,
            short height,
            double temperature,
            double humidity,
            double continentalness,
            double erosion,
            double depth,
            double weirdness,
            double pv
    ) {}

    private record StructHoverHelperCell(List<StructHoverHelperEntry> entries) {
    }

    private record StructHoverHelperEntry(BoundingBox boundingBox, PreviewSection.PreviewStruct structure) {
    }
}
