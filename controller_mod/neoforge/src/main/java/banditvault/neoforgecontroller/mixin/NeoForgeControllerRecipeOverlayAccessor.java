package banditvault.neoforgecontroller.mixin;

import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = OverlayRecipeComponent.class, remap = false)
public interface NeoForgeControllerRecipeOverlayAccessor {
    @Accessor("recipeButtons")
    List<AbstractWidget> banditvault$recipeButtons();
}
