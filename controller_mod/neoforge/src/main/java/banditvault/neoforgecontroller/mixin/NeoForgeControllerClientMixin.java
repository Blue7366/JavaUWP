package banditvault.neoforgecontroller.mixin;

import banditvault.neoforgecontroller.NeoForgeControllerCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, remap = false)
public abstract class NeoForgeControllerClientMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void banditvault$tickController(CallbackInfo ci) {
        NeoForgeControllerCompat.tick((Minecraft)(Object)this);
    }
}
