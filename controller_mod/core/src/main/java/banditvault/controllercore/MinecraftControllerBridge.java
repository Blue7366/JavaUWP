package banditvault.controllercore;

public interface MinecraftControllerBridge {
    void releaseGameplayKeys();

    void tickGameplay(ControllerState state, ControllerSettingsValues settings);

    void tickScreen(ControllerState state, ControllerSettingsValues settings);

    void applyLook(float rightX, float rightY, float seconds, ControllerSettingsValues settings);

    void renderCursor();
}
