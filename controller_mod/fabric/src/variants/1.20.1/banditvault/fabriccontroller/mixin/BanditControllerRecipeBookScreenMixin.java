package banditvault.fabriccontroller.mixin;

import banditvault.fabriccontroller.BanditControllerCompat;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_479;
import net.minecraft.class_489;
import net.minecraft.class_490;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_479.class, class_489.class, class_490.class})
public abstract class BanditControllerRecipeBookScreenMixin {
    @Inject(method = "method_25394", at = @At("TAIL"))
    private void banditvault$renderControllerCursorAboveRecipeBook(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BanditControllerCompat.renderCursor((class_437)(Object)this, context);
    }
}
