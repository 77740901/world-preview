package caeruleusTait.world.preview.client.translate;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * A {@link Language} that first asks {@link ExtraTranslations} for an override and otherwise delegates to
 * the real (vanilla + resource pack + mod) language.
 * <p>
 * Only keys starting with {@value ExtraTranslations#KEY_PREFIX} are ever intercepted, so vanilla text and
 * other mods are untouched even if an extra translation file is malformed.
 */
public final class ExtraLanguage extends Language {

    private final Language delegate;

    public ExtraLanguage(Language delegate) {
        // This wrapper sits in front of every single text lookup in the game, so never let a null delegate
        // through: fall back to the built-in default language instead.
        this.delegate = delegate == null ? Language.DEFAULT_INSTANCE : delegate;
    }

    @Override
    public String getOrDefault(String key, String fallback) {
        if (key != null && key.startsWith(ExtraTranslations.KEY_PREFIX)) {
            final String override = ExtraTranslations.uiUnchecked(key);
            if (override != null) {
                return override;
            }
        }
        return delegate.getOrDefault(key, fallback);
    }

    @Override
    public boolean has(String key) {
        if (key != null && key.startsWith(ExtraTranslations.KEY_PREFIX) && ExtraTranslations.uiUnchecked(key) != null) {
            return true;
        }
        return delegate.has(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return delegate.isDefaultRightToLeft();
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        return delegate.getVisualOrder(text);
    }
}
