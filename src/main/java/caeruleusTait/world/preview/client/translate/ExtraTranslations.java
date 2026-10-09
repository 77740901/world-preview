package caeruleusTait.world.preview.client.translate;

import caeruleusTait.world.preview.WorldPreview;
import caeruleusTait.world.preview.WorldPreviewConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static caeruleusTait.world.preview.WorldPreview.LOGGER;

/**
 * <b>Overlay translations ("More translations").</b>
 * <p>
 * This is deliberately kept <i>separate</i> from the official {@code assets/world-preview-unofficial/lang/*.json}
 * files shipped by the original author. The extra translations live in their own folder
 * ({@code assets/world-preview-unofficial/extra_lang/}) and are only applied when the user turns the
 * feature on, which makes it easy to tell "my translation" apart from "the author's translation".
 * <p>
 * Resolution is installed as a {@link Language} wrapper (see {@link ExtraLanguage}) so that every
 * {@code Component.translatable(...)} in the mod picks the override up automatically - no call sites need
 * to change, and toggling the option updates the screen on the very next frame.
 */
public final class ExtraTranslations {

    /** Config value meaning "use whatever language the game client is currently set to". */
    public static final String AUTO = "auto";

    /**
     * Safety fence: this overlay is only ever allowed to replace the mod's own keys, so a malformed file
     * can never affect vanilla or other mods' text.
     */
    public static final String KEY_PREFIX = "world-preview-unofficial.";

    private static final String ROOT = "/assets/world-preview-unofficial/extra_lang/";
    private static final String INDEX_FILE = ROOT + "index.json";
    private static final Gson GSON = new Gson();

    private static final Map<String, LocaleData> LOCALES = new LinkedHashMap<>();
    private static boolean loaded = false;
    private static Language plainDelegate = null;

    private ExtraTranslations() {
    }

    /** One translated locale, loaded from {@code extra_lang/<code>.json}. */
    public static final class LocaleData {
        public final String code;
        /** Display name of the language, as shown in the settings list (e.g. "Simplified Chinese"). */
        public final String name;
        private final Map<String, String> ui;
        private final Map<String, String> structures;

        LocaleData(String code, String name, Map<String, String> ui, Map<String, String> structures) {
            this.code = code;
            this.name = name;
            this.ui = ui;
            this.structures = structures;
        }

        public int uiCount() {
            return ui.size();
        }

        public int structureCount() {
            return structures.size();
        }
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream in = ExtraTranslations.class.getResourceAsStream(INDEX_FILE)) {
            if (in == null) {
                LOGGER.warn("Extra translations: {} not found, the feature will stay inactive", INDEX_FILE);
                return;
            }
            final JsonObject index = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            if (index == null || !index.has("locales")) {
                LOGGER.warn("Extra translations: {} has no 'locales' array", INDEX_FILE);
                return;
            }
            for (JsonElement element : index.getAsJsonArray("locales")) {
                final String code = element.getAsString();
                final LocaleData data = loadLocale(code);
                if (data != null) {
                    LOCALES.put(code, data);
                }
            }
            LOGGER.info("Extra translations: loaded {} locale(s): {}", LOCALES.size(), LOCALES.keySet());
        } catch (Exception e) {
            LOGGER.error("Extra translations: failed to read {}", INDEX_FILE, e);
        }
    }

    @Nullable
    private static LocaleData loadLocale(String code) {
        final String path = ROOT + code + ".json";
        try (InputStream in = ExtraTranslations.class.getResourceAsStream(path)) {
            if (in == null) {
                LOGGER.warn("Extra translations: {} is listed in index.json but the file is missing", path);
                return null;
            }
            final JsonObject root = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) {
                LOGGER.warn("Extra translations: {} is empty", path);
                return null;
            }
            final String name = root.has("_name") ? root.get("_name").getAsString() : code;
            return new LocaleData(
                    code,
                    name,
                    flatten(root.getAsJsonObject("ui")),
                    flatten(root.getAsJsonObject("structures"))
            );
        } catch (Exception e) {
            LOGGER.error("Extra translations: failed to parse {}", path, e);
            return null;
        }
    }

    private static Map<String, String> flatten(@Nullable JsonObject object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        final Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            final JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) {
                result.put(entry.getKey(), value.getAsString());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    @Nullable
    private static WorldPreviewConfig configOrNull() {
        try {
            final WorldPreview preview = WorldPreview.get();
            return preview == null ? null : preview.cfg();
        } catch (Throwable t) {
            return null;
        }
    }

    /** The locale that should be used right now, or {@code null} when the feature is off. */
    @Nullable
    private static LocaleData activeLocale() {
        final WorldPreviewConfig cfg = configOrNull();
        if (cfg == null || !cfg.extraTranslations) {
            return null;
        }
        ensureLoaded();
        return LOCALES.get(resolveLocale(cfg));
    }

    /**
     * Turns the configured value ({@code auto} or a locale code) into a real, available locale code.
     * <p>
     * When the desired locale has no extra translation file (e.g. the game runs {@code es_mx}), the first
     * registered locale with the same primary language ({@code es_*} here, in index.json declaration order)
     * is used instead, so regional variants still get a readable translation rather than falling all the
     * way back to English. Only an exact-match miss triggers this; {@code en_us} users are unaffected
     * because no {@code en_*} locale is registered.
     */
    public static String resolveLocale(WorldPreviewConfig cfg) {
        ensureLoaded();
        final String configured = cfg.extraTranslationLocale;
        final String wanted;
        if (configured != null && !configured.isBlank() && !AUTO.equals(configured)) {
            wanted = configured;
        } else {
            wanted = gameLanguage();
        }
        if (LOCALES.containsKey(wanted)) {
            return wanted;
        }
        final int underscore = wanted.indexOf('_');
        if (underscore > 0) {
            final String primary = wanted.substring(0, underscore) + '_';
            for (String code : LOCALES.keySet()) {
                if (code.startsWith(primary)) {
                    return code;
                }
            }
        }
        return wanted;
    }

    /** The language code the game client is currently using (e.g. {@code zh_cn}). */
    public static String gameLanguage() {
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getLanguageManager() != null) {
                final String selected = minecraft.getLanguageManager().getSelected();
                if (selected != null && !selected.isBlank()) {
                    return selected;
                }
            }
        } catch (Throwable ignored) {
            // Not on the client thread / not initialised yet - fall through.
        }
        return "en_us";
    }

    /**
     * UI string override for a translation key, or {@code null} when the feature is off, the key belongs to
     * another namespace, or this locale simply does not translate it (in which case the author's own
     * translation is used).
     */
    @Nullable
    public static String ui(String key) {
        if (key == null || !key.startsWith(KEY_PREFIX)) {
            return null;
        }
        final LocaleData locale = activeLocale();
        return locale == null ? null : locale.ui.get(key);
    }

    /** Never-throwing variant, used on the rendering path. */
    @Nullable
    static String uiUnchecked(String key) {
        try {
            return ui(key);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Structure display name override (keyed by full structure id), or {@code null}. */
    @Nullable
    public static String structure(String structureId) {
        if (structureId == null) {
            return null;
        }
        final LocaleData locale = activeLocale();
        return locale == null ? null : locale.structures.get(structureId);
    }

    /** Same as {@link #structure(String)} but safe to use while the render thread is mid-draw. */
    @Nullable
    public static String structureUnchecked(String structureId) {
        try {
            return structure(structureId);
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Locale list (for the settings dropdown)
    // ------------------------------------------------------------------

    /** All locales that ship an extra translation file, in the order declared by index.json. */
    public static List<String> availableLocales() {
        ensureLoaded();
        return new ArrayList<>(LOCALES.keySet());
    }

    @Nullable
    public static LocaleData localeData(String code) {
        ensureLoaded();
        return LOCALES.get(code);
    }

    /** Human readable name for the dropdown: the language's own name, or the "follow game" label. */
    public static Component localeName(String code) {
        if (AUTO.equals(code)) {
            return Component.translatable("world-preview-unofficial.settings.general.extra-translations.auto");
        }
        final LocaleData data = localeData(code);
        return Component.literal(data == null ? code : data.name);
    }

    /** {@code true} when at least one extra translation file could be loaded. */
    public static boolean hasAnyLocale() {
        ensureLoaded();
        return !LOCALES.isEmpty();
    }

    // ------------------------------------------------------------------
    // Installation / live refresh
    // ------------------------------------------------------------------

    /**
     * Called (through the mixin) right after Minecraft installs a language, so we can wrap it and keep the
     * real instance around as our delegate.
     */
    public static synchronized void installDelegate(Language vanilla) {
        try {
            plainDelegate = vanilla;
            Language.inject(new ExtraLanguage(vanilla));
        } catch (Throwable t) {
            LOGGER.error("Extra translations: failed to install language wrapper", t);
        }
    }

    /**
     * Force every already-built {@code TranslatableContents} to be resolved again.
     * <p>
     * {@code TranslatableContents} caches its result keyed on the {@link Language} instance identity, so
     * injecting a <i>new</i> wrapper instance is what makes the change show up immediately.
     */
    public static synchronized void invalidate() {
        try {
            Language base = plainDelegate;
            if (base == null) {
                final Language current = Language.getInstance();
                base = (current instanceof ExtraLanguage) ? Language.DEFAULT_INSTANCE : current;
                plainDelegate = base;
            }
            if (base == null) {
                return;
            }
            Language.inject(new ExtraLanguage(base));
        } catch (Throwable t) {
            LOGGER.error("Extra translations: failed to refresh language", t);
        }
    }

    /** Used by the settings checkbox. */
    public static void setEnabled(boolean enabled) {
        final WorldPreviewConfig cfg = configOrNull();
        if (cfg == null) {
            return;
        }
        cfg.extraTranslations = enabled;
        invalidate();
    }

    /** Used by the language dropdown. */
    public static void setLocale(String code) {
        final WorldPreviewConfig cfg = configOrNull();
        if (cfg == null) {
            return;
        }
        cfg.extraTranslationLocale = code;
        invalidate();
    }
}
