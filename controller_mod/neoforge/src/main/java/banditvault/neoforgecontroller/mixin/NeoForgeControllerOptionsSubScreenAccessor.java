package banditvault.neoforgecontroller.mixin;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OptionsSubScreen.class, remap = false)
public interface NeoForgeControllerOptionsSubScreenAccessor {
    @Accessor("list")
    OptionsList banditvault$optionsList();
}
