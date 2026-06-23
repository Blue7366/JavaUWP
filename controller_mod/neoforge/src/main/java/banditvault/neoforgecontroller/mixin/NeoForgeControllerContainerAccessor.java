package banditvault.neoforgecontroller.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public interface NeoForgeControllerContainerAccessor {
    @Invoker("slotClicked")
    void banditvault$slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType);
}
