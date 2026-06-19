package banditvault.controllercore;

public final class ControllerSettingsValues {
    public final boolean toggleCrouch;
    public final boolean toggleSprint;
    public final boolean invertY;
    public final float lookSpeed;
    public final double cursorSpeed;
    public final double scrollAmount;
    public final float moveDeadzone;
    public final float lookDeadzone;
    public final float cursorDeadzone;
    public final float triggerDeadzone;

    public ControllerSettingsValues(
        boolean toggleCrouch,
        boolean toggleSprint,
        boolean invertY,
        float lookSpeed,
        double cursorSpeed,
        double scrollAmount,
        float moveDeadzone,
        float lookDeadzone,
        float cursorDeadzone,
        float triggerDeadzone) {
        this.toggleCrouch = toggleCrouch;
        this.toggleSprint = toggleSprint;
        this.invertY = invertY;
        this.lookSpeed = lookSpeed;
        this.cursorSpeed = cursorSpeed;
        this.scrollAmount = scrollAmount;
        this.moveDeadzone = moveDeadzone;
        this.lookDeadzone = lookDeadzone;
        this.cursorDeadzone = cursorDeadzone;
        this.triggerDeadzone = triggerDeadzone;
    }
}
