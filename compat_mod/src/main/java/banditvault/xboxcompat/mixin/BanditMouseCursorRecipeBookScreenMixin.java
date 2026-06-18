package banditvault.xboxcompat.mixin;

import banditvault.xboxcompat.BanditMouseCursorOverlay;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_10260;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_465.class, class_10260.class})
public abstract class BanditMouseCursorRecipeBookScreenMixin {
    @Inject(method = "method_25394", at = @At("TAIL"), require = 0)
    private void banditvault$renderMouseCursorAboveRecipeBook(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BanditMouseCursorOverlay.render((class_437)(Object)this, context, mouseX, mouseY);
    }
}
