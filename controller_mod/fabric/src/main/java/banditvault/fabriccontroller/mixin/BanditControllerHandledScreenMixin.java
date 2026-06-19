package banditvault.fabriccontroller.mixin;

import banditvault.fabriccontroller.BanditControllerCompat;
import net.minecraft.class_465;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(class_465.class)
public abstract class BanditControllerHandledScreenMixin {
    @ModifyVariable(method = "method_25394", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int banditvault$useControllerMouseX(int mouseX) {
        return BanditControllerCompat.screenMouseX(mouseX);
    }

    @ModifyVariable(method = "method_25394", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int banditvault$useControllerMouseY(int mouseY) {
        return BanditControllerCompat.screenMouseY(mouseY);
    }
}
