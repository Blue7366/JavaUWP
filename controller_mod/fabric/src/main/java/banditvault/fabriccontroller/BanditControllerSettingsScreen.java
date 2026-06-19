package banditvault.fabriccontroller;

import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_2561;
import org.lwjgl.glfw.GLFW;

public final class BanditControllerSettingsScreen extends class_437 {
    private final class_437 parent;

    public BanditControllerSettingsScreen(class_437 parent) {
        super(class_2561.method_30163("Bandit Controller"));
        this.parent = parent;
    }

    @Override
    protected void method_25426() {
        rebuildButtons();
    }

    @Override
    public void method_25394(class_4587 matrices, int mouseX, int mouseY, float delta) {
        this.method_25420(matrices);
        class_332.method_27534(matrices, this.field_22793, class_2561.method_30163("Bandit Controller"), this.field_22789 / 2, 18, 0xFFFFFF);
        super.method_25394(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public void close() {
        BanditControllerSettings.save();
        class_310.method_1551().method_1507(parent);
    }

    private void rebuildButtons() {
        clearWidgets();
        BanditControllerSettings settings = BanditControllerSettings.get();
        int x = this.field_22789 / 2 - 105;
        int y = 42;
        addButton(newButton(x, y, 210, 20, crouchLabel(settings), button -> {
            settings.toggleCrouch = !settings.toggleCrouch;
            BanditControllerSettings.save();
            rebuildButtons();
        }));
        y += 24;
        addButton(newButton(x, y, 210, 20, sprintLabel(settings), button -> {
            settings.toggleSprint = !settings.toggleSprint;
            BanditControllerSettings.save();
            rebuildButtons();
        }));
        y += 24;
        addButton(newButton(x, y, 210, 20, invertLabel(settings), button -> {
            settings.invertY = !settings.invertY;
            BanditControllerSettings.save();
            rebuildButtons();
        }));
        y += 28;
        addStepper(x, y, "Look speed", Math.round(settings.lookSpeed), -15.0, 15.0, 30.0, 300.0, value -> settings.lookSpeed = (float)value);
        y += 24;
        addStepper(x, y, "Cursor speed", settings.cursorSpeed, -1.0, 1.0, 4.0, 40.0, value -> settings.cursorSpeed = value);
        y += 24;
        addStepper(x, y, "Scroll speed", settings.scrollAmount, -0.25, 0.25, 0.25, 4.0, value -> settings.scrollAmount = value);
        y += 24;
        addStepper(x, y, "Move deadzone", settings.moveDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.moveDeadzone = (float)value);
        y += 24;
        addStepper(x, y, "Look deadzone", settings.lookDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.lookDeadzone = (float)value);
        y += 24;
        addStepper(x, y, "Cursor deadzone", settings.cursorDeadzone, -0.05, 0.05, 0.0, 0.75, value -> settings.cursorDeadzone = (float)value);
        y += 30;
        addButton(newButton(x, y, 210, 20, "Done", button -> close()));
    }

    private void addStepper(int x, int y, String label, double value, double down, double up, double min, double max, Setter setter) {
        addButton(newButton(x, y, 42, 20, "-", button -> adjust(value, down, min, max, setter)));
        addButton(newButton(x + 46, y, 118, 20, label + ": " + format(value), button -> { }));
        addButton(newButton(x + 168, y, 42, 20, "+", button -> adjust(value, up, min, max, setter)));
    }

    private void adjust(double value, double delta, double min, double max, Setter setter) {
        double next = value + delta;
        if (next < min) {
            next = min;
        }
        if (next > max) {
            next = max;
        }
        setter.set(next);
        BanditControllerSettings.save();
        rebuildButtons();
    }

    private class_4185 newButton(int x, int y, int w, int h, String label, class_4185.class_4241 press) {
        return new class_4185(x, y, w, h, class_2561.method_30163(label), press);
    }

    private void addButton(class_4185 button) {
        if (invokeWidgetMethod("method_37063", button)) {
            return;
        }
        if (invokeWidgetMethod("method_25411", button)) {
            return;
        }
        invokeWidgetMethod("method_25429", button);
    }

    private boolean invokeWidgetMethod(String name, Object widget) {
        Class<?> current = getClass();
        while (current != null) {
            java.lang.reflect.Method[] methods = current.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                java.lang.reflect.Method method = methods[i];
                if (!method.getName().equals(name) || method.getParameterTypes().length != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(widget.getClass())) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    method.invoke(this, widget);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private void clearWidgets() {
        if (invokeNoArg("method_37067")) {
            return;
        }
        try {
            java.lang.reflect.Field children = findField(getClass(), "field_22786");
            children.setAccessible(true);
            ((java.util.List<?>)children.get(this)).clear();
            java.lang.reflect.Field buttons = findField(getClass(), "field_22791");
            buttons.setAccessible(true);
            ((java.util.List<?>)buttons.get(this)).clear();
        } catch (Throwable ignored) {
        }
    }

    private boolean invokeNoArg(String name) {
        Class<?> current = getClass();
        while (current != null) {
            try {
                java.lang.reflect.Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(this);
                return true;
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    private java.lang.reflect.Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private String crouchLabel(BanditControllerSettings settings) {
        return "Crouch: " + (settings.toggleCrouch ? "Toggle" : "Hold");
    }

    private String sprintLabel(BanditControllerSettings settings) {
        return "Sprint: " + (settings.toggleSprint ? "Toggle" : "Hold");
    }

    private String invertLabel(BanditControllerSettings settings) {
        return "Invert Y: " + (settings.invertY ? "On" : "Off");
    }

    private String format(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private interface Setter {
        void set(double value);
    }
}
