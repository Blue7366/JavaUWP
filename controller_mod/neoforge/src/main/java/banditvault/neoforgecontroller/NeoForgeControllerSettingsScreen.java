package banditvault.neoforgecontroller;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeControllerSettingsScreen extends Screen {
    private final Screen parent;

    public NeoForgeControllerSettingsScreen(Screen parent) {
        super(NeoForgeControllerCompat.textLiteral("Bandit Controller"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, "Bandit Controller", this.width / 2, 18, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        close();
    }

    public void close() {
        NeoForgeControllerSettings.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void rebuildButtons() {
        clearWidgets();
        NeoForgeControllerSettings settings = NeoForgeControllerSettings.get();
        int panelWidth = Math.min(426, Math.max(250, this.width - 24));
        int x = this.width / 2 - panelWidth / 2;
        int y = this.height < 260 ? 32 : 42;
        int row = this.height < 260 ? 21 : 24;

        if (panelWidth >= 390) {
            int leftWidth = 158;
            int gap = 12;
            int rightX = x + leftWidth + gap;
            int rightWidth = panelWidth - leftWidth - gap;
            int leftY = y;
            int rightY = y;

            addToggle(x, leftY, leftWidth, crouchLabel(settings), () -> settings.toggleCrouch = !settings.toggleCrouch);
            leftY += row;
            addToggle(x, leftY, leftWidth, sprintLabel(settings), () -> settings.toggleSprint = !settings.toggleSprint);
            leftY += row;
            addToggle(x, leftY, leftWidth, invertLabel(settings), () -> settings.invertY = !settings.invertY);
            leftY += row + 4;
            addButton(NeoForgeControllerCompat.createButton(x, leftY, leftWidth, 20, "Reset defaults", button -> reset(settings)));

            addStepper(rightX, rightY, rightWidth, "Look", Math.round(settings.lookSpeed), -15.0, 15.0, 30.0, 300.0, value -> settings.lookSpeed = (float)value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Cursor", settings.cursorSpeed, -2.0, 2.0, 4.0, 60.0, value -> settings.cursorSpeed = value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Scroll", settings.scrollAmount, -0.25, 0.25, 0.25, 4.0, value -> settings.scrollAmount = value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Move dz", settings.moveDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.moveDeadzone = (float)value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Look dz", settings.lookDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.lookDeadzone = (float)value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Cursor dz", settings.cursorDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.cursorDeadzone = (float)value);
            rightY += row;
            addStepper(rightX, rightY, rightWidth, "Trigger dz", settings.triggerDeadzone, -0.05, 0.05, 0.0, 0.95, value -> settings.triggerDeadzone = (float)value);
        } else {
            addToggle(x, y, panelWidth, crouchLabel(settings), () -> settings.toggleCrouch = !settings.toggleCrouch);
            y += row;
            addToggle(x, y, panelWidth, sprintLabel(settings), () -> settings.toggleSprint = !settings.toggleSprint);
            y += row;
            addToggle(x, y, panelWidth, invertLabel(settings), () -> settings.invertY = !settings.invertY);
            y += row + 4;
            addStepper(x, y, panelWidth, "Look", Math.round(settings.lookSpeed), -15.0, 15.0, 30.0, 300.0, value -> settings.lookSpeed = (float)value);
            y += row;
            addStepper(x, y, panelWidth, "Cursor", settings.cursorSpeed, -2.0, 2.0, 4.0, 60.0, value -> settings.cursorSpeed = value);
            y += row;
            addStepper(x, y, panelWidth, "Move dz", settings.moveDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.moveDeadzone = (float)value);
            y += row;
            addButton(NeoForgeControllerCompat.createButton(x, y, panelWidth, 20, "Reset defaults", button -> reset(settings)));
        }

        int doneWidth = Math.min(210, panelWidth);
        int doneY = Math.max(4, this.height - 26);
        addButton(NeoForgeControllerCompat.createButton(this.width / 2 - doneWidth / 2, doneY, doneWidth, 20, "Done", button -> close()));
    }

    private void addToggle(int x, int y, int width, String label, Runnable action) {
        addButton(NeoForgeControllerCompat.createButton(x, y, width, 20, label, button -> {
            action.run();
            saveAndRebuild();
        }));
    }

    private void addStepper(int x, int y, int width, String label, double value, double down, double up, double min, double max, Setter setter) {
        int buttonWidth = 36;
        int gap = 6;
        int labelWidth = Math.max(82, width - buttonWidth * 2 - gap * 2);
        addButton(NeoForgeControllerCompat.createButton(x, y, buttonWidth, 20, "-", button -> adjust(value, down, min, max, setter)));
        addButton(NeoForgeControllerCompat.createButton(x + buttonWidth + gap, y, labelWidth, 20, label + ": " + format(value), button -> {}));
        addButton(NeoForgeControllerCompat.createButton(x + buttonWidth + gap + labelWidth + gap, y, buttonWidth, 20, "+", button -> adjust(value, up, min, max, setter)));
    }

    private void adjust(double value, double delta, double min, double max, Setter setter) {
        setter.set(Math.max(min, Math.min(max, value + delta)));
        saveAndRebuild();
    }

    private void addButton(Button button) {
        addRenderableWidget(button);
    }

    private void reset(NeoForgeControllerSettings settings) {
        settings.toggleCrouch = false;
        settings.toggleSprint = false;
        settings.invertY = false;
        settings.lookSpeed = 135.0f;
        settings.cursorSpeed = 18.0;
        settings.scrollAmount = 1.0;
        settings.moveDeadzone = 0.35f;
        settings.lookDeadzone = 0.12f;
        settings.cursorDeadzone = 0.12f;
        settings.triggerDeadzone = 0.25f;
        saveAndRebuild();
    }

    private void saveAndRebuild() {
        NeoForgeControllerSettings.save();
        rebuildButtons();
    }

    private String crouchLabel(NeoForgeControllerSettings settings) {
        return "Crouch: " + (settings.toggleCrouch ? "Toggle" : "Hold");
    }

    private String sprintLabel(NeoForgeControllerSettings settings) {
        return "Sprint: " + (settings.toggleSprint ? "Toggle" : "Hold");
    }

    private String invertLabel(NeoForgeControllerSettings settings) {
        return "Invert Y: " + (settings.invertY ? "On" : "Off");
    }

    private String format(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private interface Setter {
        void set(double value);
    }
}
