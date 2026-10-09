package caeruleusTait.world.preview.mixin.client;

import caeruleusTait.world.preview.client.translate.ExtraLanguage;
import caeruleusTait.world.preview.client.translate.ExtraTranslations;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks {@link Language#inject(Language)} so that the "More translations" overlay keeps its hands on the
 * real language.
 * <p>
 * {@code Language.inject} is only a setter for the private static {@code instance} field, so wrapping the
 * value on the way in is enough. Note that the wrapper itself is re-injected through the same method -
 * {@link ExtraLanguage} instances are recognised and left alone, which stops the recursion.
 */
@Mixin(Language.class)
public class LanguageMixin {

    @Inject(method = "inject", at = @At("TAIL"))
    private static void worldpreview$wrapInjectedLanguage(Language language, CallbackInfo ci) {
        if (!(language instanceof ExtraLanguage)) {
            ExtraTranslations.installDelegate(language);
        }
    }
}
