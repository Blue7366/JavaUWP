package banditvault.neoforgecontroller.mixin;

import banditvault.neoforgecontroller.NeoForgeControllerCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public abstract class NeoForgeControllerHandledScreenMixin {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void banditvault$updateControllerCursor(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NeoForgeControllerCompat.updateScreenCursorBeforeRender((net.minecraft.client.gui.screens.Screen) (Object) this, mouseX, mouseY);
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false)
    private int banditvault$useControllerMouseX(int mouseX) {
        return NeoForgeControllerCompat.screenMouseX(mouseX);
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 1, argsOnly = true, remap = false)
    private int banditvault$useControllerMouseY(int mouseY) {
        return NeoForgeControllerCompat.screenMouseY(mouseY);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void banditvault$renderControllerCursor(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NeoForgeControllerCompat.renderCursor((net.minecraft.client.gui.screens.Screen) (Object) this, graphics);
    }
}
