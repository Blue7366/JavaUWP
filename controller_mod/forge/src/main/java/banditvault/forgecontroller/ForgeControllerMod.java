package banditvault.forgecontroller;

import net.minecraftforge.fml.common.Mod;

@Mod(ForgeControllerMod.MOD_ID)
public final class ForgeControllerMod {
    public static final String MOD_ID = "banditvault_forge_controller";

    public ForgeControllerMod() {
        ForgeControllerLog.log("Bandit Forge controller mod loaded (1.0.4)");
        ForgeControllerCompat.ensureInitialized();
    }
}
