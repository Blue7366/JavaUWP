package banditvault.neoforgecontroller;

import net.neoforged.fml.common.Mod;

@Mod(NeoForgeControllerMod.MOD_ID)
public final class NeoForgeControllerMod {
    public static final String MOD_ID = "banditvault_neoforge_controller";

    public NeoForgeControllerMod() {
        NeoForgeControllerLog.log("Bandit NeoForge controller mod loaded (1.0.0, Minecraft 1.21.1)");
        NeoForgeControllerSettings.load();
        NeoForgeControllerCompat.ensureInitialized();
    }
}
