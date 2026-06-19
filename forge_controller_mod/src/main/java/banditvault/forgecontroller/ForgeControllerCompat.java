package banditvault.forgecontroller;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

public final class ForgeControllerCompat {
    private static final int GAMEPAD_ID = GLFW.GLFW_JOYSTICK_1;
    private static final int LEFT_CLICK = 0;
    private static final int RIGHT_CLICK = 1;

    private static final GLFWGamepadState STATE = GLFWGamepadState.create();
    private static final boolean[] LAST_BUTTONS = new boolean[15];
    private static final boolean[] LAST_TRIGGER_HELD = new boolean[2];
    private static final ReflectionCache REFLECTION = new ReflectionCache();

    private static boolean active;
    private static boolean loggedReady;
    private static boolean loggedInventoryReflectionFailure;
    private static boolean loggedScreenReflectionFailure;
    private static boolean loggedSettingsReflectionFailure;
    private static boolean loggedNoGamepad;
    private static boolean loggedInit;
    private static long tickCount;
    private static boolean crouchToggled;
    private static boolean sprintToggled;
    private static double cursorX = -1.0;
    private static double cursorY = -1.0;
    private static int scrollCooldown;
    private static long lastLookNanos;
    private static long renderFrameActiveNanos;
    private static boolean loggedLookApplied;
    private static boolean followingCompanion;
    private static Object lastCompanionScreen;
    private static double lastCompanionMouseX;
    private static double lastCompanionMouseY;
    private static long companionActiveUntilNanos;

    private static final long COMPANION_IDLE_NANOS = 500_000_000L;
    private static final double COMPANION_MOVE_EPSILON = 0.5;

    private ForgeControllerCompat() {
    }

    public static void markRenderFrameActive() {
        renderFrameActiveNanos = System.nanoTime();
    }

    public static void ensureInitialized() {
        if (loggedInit) {
            return;
        }
        loggedInit = true;
        ForgeControllerLog.log("Forge controller compat initialized");
    }

    public static void tick(Minecraft client) {
        ensureInitialized();
        tickCount++;
        if (client == null) {
            return;
        }

        if (!poll()) {
            if (!loggedNoGamepad || tickCount <= 5 || tickCount % 600 == 0) {
                loggedNoGamepad = true;
                ForgeControllerLog.log("No GLFW gamepad detected on joystick 1 (tick=" + tickCount + ")");
            }
            if (active) {
                releaseGameplayKeys(client);
                crouchToggled = false;
                sprintToggled = false;
                followingCompanion = false;
                active = false;
            }
            return;
        }
        loggedNoGamepad = false;

        active = true;
        if (!loggedReady) {
            loggedReady = true;
            ForgeControllerLog.log("Forge controller compat active");
        }

        if (client.f_91080_ != null) {
            lastLookNanos = 0L;
            releaseGameplayKeys(client);
            tickScreen(client, client.f_91080_);
        } else {
            followingCompanion = false;
            tickGameplay(client);
        }

        copyButtons();
    }

    public static void renderFrame(Minecraft client) {
        if (client == null || client.f_91080_ != null || client.f_91074_ == null || !poll()) {
            lastLookNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        float seconds = 1.0f / 60.0f;
        if (lastLookNanos != 0L) {
            seconds = (now - lastLookNanos) / 1000000000.0f;
            if (seconds < 1.0f / 240.0f) {
                seconds = 1.0f / 240.0f;
            } else if (seconds > 1.0f / 20.0f) {
                seconds = 1.0f / 20.0f;
            }
        }
        lastLookNanos = now;

        ForgeControllerSettings settings = ForgeControllerSettings.get();
        float y = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
        if (settings.invertY) {
            y = -y;
        }
        applyLook(client.f_91074_, axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X), y, seconds, settings);
    }

    public static void renderCursor(Screen screen, GuiGraphics graphics) {
        if (!active || followingCompanion || screen == null || graphics == null || cursorX < 0.0 || cursorY < 0.0) {
            return;
        }
        int x = (int) Math.round(cursorX);
        int y = (int) Math.round(cursorY);
        graphics.m_280509_(x - 3, y - 3, x + 4, y + 4, 0x66000000);
        graphics.m_280509_(x - 5, y, x + 6, y + 1, 0xFFFFFFFF);
        graphics.m_280509_(x, y - 5, x + 1, y + 6, 0xFFFFFFFF);
    }

    public static int screenMouseX(int fallback) {
        if (!active || cursorX < 0.0) {
            return fallback;
        }
        return (int) Math.round(cursorX);
    }

    public static int screenMouseY(int fallback) {
        if (!active || cursorY < 0.0) {
            return fallback;
        }
        return (int) Math.round(cursorY);
    }

    private static boolean poll() {
        try {
            return GLFW.glfwJoystickIsGamepad(GAMEPAD_ID) && GLFW.glfwGetGamepadState(GAMEPAD_ID, STATE);
        } catch (Throwable t) {
            if (!loggedReady) {
                loggedReady = true;
                ForgeControllerLog.logException("Forge controller compat failed to poll GLFW gamepad", t);
            }
            return false;
        }
    }

    private static void tickGameplay(Minecraft client) {
        ForgeControllerSettings settings = ForgeControllerSettings.get();
        Options options = client.f_91066_;
        if (options == null) {
            return;
        }
        ForgeControllerKeys keys = new ForgeControllerKeys(options);

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_BACK)) {
            client.m_91152_(new ForgeControllerSettingsScreen(null));
            return;
        }

        float lx = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        float ly = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        setHeld(keys.forward, ly < -settings.moveDeadzone);
        setHeld(keys.back, ly > settings.moveDeadzone);
        setHeld(keys.left, lx < -settings.moveDeadzone);
        setHeld(keys.right, lx > settings.moveDeadzone);
        setHeld(keys.jump, button(GLFW.GLFW_GAMEPAD_BUTTON_A));

        boolean sneakHeld;
        if (settings.toggleCrouch) {
            if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_B)) {
                crouchToggled = !crouchToggled;
            }
            sneakHeld = crouchToggled;
        } else {
            crouchToggled = false;
            sneakHeld = button(GLFW.GLFW_GAMEPAD_BUTTON_B);
        }
        setHeld(keys.sneak, sneakHeld);
        setSneakInput(client.f_91074_, sneakHeld);

        setHeld(keys.attack, trigger(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER));
        setHeld(keys.use, trigger(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER));

        if (settings.toggleSprint) {
            if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB)) {
                sprintToggled = !sprintToggled;
            }
            setHeld(keys.sprint, sprintToggled);
        } else {
            sprintToggled = false;
            setHeld(keys.sprint, button(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB));
        }

        if (triggerPressed(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)) {
            pressKey(keys.attack);
        }
        if (triggerPressed(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER)) {
            pressKey(keys.use);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_Y)) {
            pressKey(keys.inventory);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_X)) {
            pressKey(keys.swapOffhand);
        }

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER)) {
            changeHotbarSlot(client.f_91074_, -1);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)) {
            changeHotbarSlot(client.f_91074_, 1);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_START)) {
            Method pause = findMethod(Minecraft.class, "m_91358_", boolean.class);
            if (pause != null) {
                invokeMethod(pause, client, false);
            }
        }

        if (!renderFramePathActive()) {
            float y = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
            if (settings.invertY) {
                y = -y;
            }
            applyLook(client.f_91074_, axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X), y, 1.0f / 20.0f, settings);
        }
    }

    private static void tickScreen(Minecraft client, Screen screen) {
        ForgeControllerSettings settings = ForgeControllerSettings.get();
        if (cursorX < 0.0 || cursorY < 0.0) {
            cursorX = Math.max(1, screen.f_96543_ / 2);
            cursorY = Math.max(1, screen.f_96544_ / 2);
        }

        float lx = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        float ly = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        float ry = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);

        if (updateCompanion(client, screen)) {
            followCompanionCursor(client, screen);
        } else {
            double dx = shapedCursorAxis(lx, settings.cursorDeadzone);
            double dy = shapedCursorAxis(ly, settings.cursorDeadzone);
            cursorX = clamp(cursorX + dx * settings.cursorSpeed, 0.0, Math.max(1, screen.f_96543_ - 1));
            cursorY = clamp(cursorY + dy * settings.cursorSpeed, 0.0, Math.max(1, screen.f_96544_ - 1));
            invokeScreenMouseMoved(screen, cursorX, cursorY);
        }

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_BACK)) {
            if (screen instanceof ForgeControllerSettingsScreen) {
                ((ForgeControllerSettingsScreen) screen).close();
            } else {
                client.m_91152_(new ForgeControllerSettingsScreen(screen));
            }
            return;
        }

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_A)) {
            invokeScreenMousePressed(screen, cursorX, cursorY, LEFT_CLICK);
        }
        if (released(GLFW.GLFW_GAMEPAD_BUTTON_A)) {
            invokeScreenMouseReleased(screen, cursorX, cursorY, LEFT_CLICK);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_X)) {
            invokeScreenMousePressed(screen, cursorX, cursorY, RIGHT_CLICK);
        }
        if (released(GLFW.GLFW_GAMEPAD_BUTTON_X)) {
            invokeScreenMouseReleased(screen, cursorX, cursorY, RIGHT_CLICK);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_B)) {
            if (!invokeScreenKeyPressed(screen, GLFW.GLFW_KEY_ESCAPE, 0, 0)) {
                client.m_91152_(null);
            }
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_Y)) {
            quickMoveFocusedSlot(screen);
        }

        if (scrollCooldown > 0) {
            scrollCooldown--;
        }
        if (scrollCooldown == 0) {
            double scroll = 0.0;
            if (ry < -0.35f || button(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP)) {
                scroll = settings.scrollAmount;
            } else if (ry > 0.35f || button(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN)) {
                scroll = -settings.scrollAmount;
            }
            if (scroll != 0.0) {
                invokeScreenMouseScrolled(screen, cursorX, cursorY, scroll);
                scrollCooldown = 5;
            }
        }
    }

    private static boolean updateCompanion(Minecraft client, Screen screen) {
        double mouseX;
        double mouseY;
        try {
            mouseX = client.f_91067_.m_91589_();
            mouseY = client.f_91067_.m_91594_();
        } catch (Throwable t) {
            followingCompanion = false;
            return false;
        }

        long now = System.nanoTime();
        if (screen != lastCompanionScreen) {
            lastCompanionScreen = screen;
            lastCompanionMouseX = mouseX;
            lastCompanionMouseY = mouseY;
        } else if (Math.abs(mouseX - lastCompanionMouseX) > COMPANION_MOVE_EPSILON
            || Math.abs(mouseY - lastCompanionMouseY) > COMPANION_MOVE_EPSILON) {
            lastCompanionMouseX = mouseX;
            lastCompanionMouseY = mouseY;
            companionActiveUntilNanos = now + COMPANION_IDLE_NANOS;
        }

        boolean companion = now <= companionActiveUntilNanos;
        followingCompanion = companion;
        return companion;
    }

    private static void followCompanionCursor(Minecraft client, Screen screen) {
        Window window = client.m_91268_();
        double rawX = client.f_91067_.m_91589_();
        double rawY = client.f_91067_.m_91594_();
        double scaledX = rawX * screen.f_96543_ / Math.max(1.0, window.m_85441_());
        double scaledY = rawY * screen.f_96544_ / Math.max(1.0, window.m_85442_());
        cursorX = clamp(scaledX, 0.0, Math.max(1, screen.f_96543_ - 1));
        cursorY = clamp(scaledY, 0.0, Math.max(1, screen.f_96544_ - 1));
        invokeScreenMouseMoved(screen, cursorX, cursorY);
    }

    private static void applyLook(LocalPlayer player, float rx, float ry, float seconds, ForgeControllerSettings settings) {
        float lookX = shapedLookAxis(rx, settings.lookDeadzone);
        float lookY = shapedLookAxis(ry, settings.lookDeadzone);
        if (lookX == 0.0f && lookY == 0.0f) {
            return;
        }
        float scale = settings.lookSpeed * seconds;
        player.m_146922_(player.m_146908_() + lookX * scale);
        player.m_146926_((float) clamp(player.m_146909_() + lookY * scale, -90.0, 90.0));
        if (!loggedLookApplied) {
            loggedLookApplied = true;
            ForgeControllerLog.log(String.format(
                "Applied controller look rx=%.2f ry=%.2f scale=%.2f",
                lookX,
                lookY,
                scale));
        }
    }

    private static boolean renderFramePathActive() {
        return renderFrameActiveNanos != 0L
            && System.nanoTime() - renderFrameActiveNanos < 100_000_000L;
    }

    private static void releaseGameplayKeys(Minecraft client) {
        Options options = client.f_91066_;
        if (options == null) {
            return;
        }
        ForgeControllerKeys keys = new ForgeControllerKeys(options);
        setHeld(keys.forward, false);
        setHeld(keys.back, false);
        setHeld(keys.left, false);
        setHeld(keys.right, false);
        setHeld(keys.jump, false);
        setHeld(keys.sneak, false);
        setSneakInput(client.f_91074_, false);
        setHeld(keys.sprint, false);
        setHeld(keys.attack, false);
        setHeld(keys.use, false);
    }

    private static void setHeld(KeyMapping key, boolean held) {
        if (key == null) {
            return;
        }
        key.m_7249_(held);
        InputConstants.Key inputKey = key.getKey();
        if (inputKey != null) {
            KeyMapping.m_90837_(inputKey, held);
        }
    }

    private static void setSneakInput(LocalPlayer player, boolean sneak) {
        if (player == null || player.f_108618_ == null) {
            return;
        }
        player.f_108618_.f_108573_ = sneak;
    }

    private static void pressKey(KeyMapping key) {
        if (key == null) {
            return;
        }
        InputConstants.Key inputKey = key.getKey();
        if (inputKey != null) {
            KeyMapping.m_90835_(inputKey);
        }
    }

    private static void changeHotbarSlot(LocalPlayer player, int direction) {
        if (player == null) {
            return;
        }
        if (REFLECTION.getInventoryMethod == null || REFLECTION.selectedSlotField == null) {
            return;
        }
        try {
            Object inventory = REFLECTION.getInventoryMethod.invoke(player);
            int slot = REFLECTION.selectedSlotField.getInt(inventory);
            slot = (slot + direction) % 9;
            if (slot < 0) {
                slot += 9;
            }
            REFLECTION.selectedSlotField.setInt(inventory, slot);
        } catch (Throwable t) {
            if (!loggedInventoryReflectionFailure) {
                loggedInventoryReflectionFailure = true;
                ForgeControllerLog.logException("Forge controller compat failed to change hotbar slot", t);
            }
        }
    }

    private static void quickMoveFocusedSlot(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen)) {
            return;
        }
        if (REFLECTION.hoveredSlotField == null || REFLECTION.slotClickedMethod == null) {
            return;
        }
        try {
            Slot slot = (Slot) REFLECTION.hoveredSlotField.get(screen);
            if (slot == null || !slot.m_6657_()) {
                return;
            }
            REFLECTION.slotClickedMethod.invoke(screen, slot, slot.f_40219_, 0, ClickType.QUICK_MOVE);
        } catch (Throwable t) {
            if (!loggedScreenReflectionFailure) {
                loggedScreenReflectionFailure = true;
                ForgeControllerLog.logException("Forge controller compat failed to quick-move focused slot", t);
            }
        }
    }

    private static void invokeScreenMouseMoved(Screen screen, double x, double y) {
        invokeMethod(REFLECTION.mouseMovedMethod, screen, x, y);
    }

    private static void invokeScreenMousePressed(Screen screen, double x, double y, int button) {
        invokeMethod(REFLECTION.mouseClickedMethod, screen, x, y, button);
    }

    private static void invokeScreenMouseReleased(Screen screen, double x, double y, int button) {
        invokeMethod(REFLECTION.mouseReleasedMethod, screen, x, y, button);
    }

    private static void invokeScreenMouseScrolled(Screen screen, double x, double y, double scroll) {
        invokeMethod(REFLECTION.mouseScrolledMethod, screen, x, y, scroll);
    }

    private static boolean invokeScreenKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        Object value = invokeMethod(REFLECTION.keyPressedMethod, screen, keyCode, scanCode, modifiers);
        return value instanceof Boolean && (Boolean) value;
    }

    private static Object invokeMethod(Method method, Object target, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float axis(int index) {
        return clampAxis(STATE.axes(index));
    }

    private static boolean trigger(int index) {
        return axis(index) > ForgeControllerSettings.get().triggerDeadzone;
    }

    private static boolean triggerPressed(int index) {
        return trigger(index) && !LAST_TRIGGER_HELD[index - GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER];
    }

    private static boolean button(int index) {
        return STATE.buttons(index) == GLFW.GLFW_PRESS;
    }

    private static boolean pressed(int index) {
        return button(index) && !LAST_BUTTONS[index];
    }

    private static boolean released(int index) {
        return !button(index) && LAST_BUTTONS[index];
    }

    private static void copyButtons() {
        for (int i = 0; i < LAST_BUTTONS.length; i++) {
            LAST_BUTTONS[i] = button(i);
        }
        LAST_TRIGGER_HELD[0] = trigger(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER);
        LAST_TRIGGER_HELD[1] = trigger(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER);
    }

    private static float shapedLookAxis(float value, float deadzone) {
        value = clampAxis(value);
        float abs = Math.abs(value);
        if (abs < deadzone) {
            return 0.0f;
        }
        float shaped = (abs - deadzone) / (1.0f - deadzone);
        shaped = shaped * shaped;
        return Math.copySign(shaped, value);
    }

    private static double shapedCursorAxis(float value, float deadzone) {
        value = clampAxis(value);
        float abs = Math.abs(value);
        if (abs < deadzone) {
            return 0.0;
        }
        return Math.copySign((abs - deadzone) / (1.0f - deadzone), value);
    }

    private static float clampAxis(float value) {
        if (value < -1.0f) {
            return -1.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            for (Class<?> iface : current.getInterfaces()) {
                Method method = findMethod(iface, name, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static final class ReflectionCache {
        private boolean loggedMissingBindings;

        private final Method getInventoryMethod = findMethod(LocalPlayer.class, "m_150109_");
        private final Field selectedSlotField = findField(findClass("net.minecraft.world.entity.player.Inventory"), "f_35977_");
        private final Field hoveredSlotField = findField(AbstractContainerScreen.class, "f_97734_");
        private final Method slotClickedMethod = findMethod(AbstractContainerScreen.class, "m_6597_",
            Slot.class, int.class, int.class, ClickType.class);
        private final Method mouseMovedMethod = findMethod(Screen.class, "m_5953_", double.class, double.class);
        private final Method mouseClickedMethod = findInterfaceMethod(
            "net.minecraft.client.gui.components.events.ContainerEventHandler",
            "m_6375_", double.class, double.class, int.class);
        private final Method mouseReleasedMethod = findInterfaceMethod(
            "net.minecraft.client.gui.components.events.ContainerEventHandler",
            "m_6348_", double.class, double.class, int.class);
        private final Method mouseScrolledMethod = findInterfaceMethod(
            "net.minecraft.client.gui.components.events.ContainerEventHandler",
            "m_6050_", double.class, double.class, double.class);
        private final Method keyPressedMethod = findMethod(Screen.class, "m_7933_", int.class, int.class, int.class);

        private ReflectionCache() {
            logMissingBindingsOnce();
        }

        private static Class<?> findClass(String className) {
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static Method findInterfaceMethod(String className, String methodName, Class<?>... parameterTypes) {
            Class<?> type = findClass(className);
            if (type == null) {
                return null;
            }
            return findMethod(type, methodName, parameterTypes);
        }

        private void logMissingBindingsOnce() {
            if (loggedMissingBindings) {
                return;
            }
            loggedMissingBindings = true;
            if (mouseClickedMethod == null || mouseReleasedMethod == null || mouseScrolledMethod == null || mouseMovedMethod == null) {
                ForgeControllerLog.log("Forge controller screen input bindings incomplete; menu cursor clicks may be limited");
            }
        }
    }

    public static Component textLiteral(String text) {
        try {
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = findMethod(componentClass, "m_237113_", String.class);
            if (literal != null) {
                Object value = literal.invoke(null, text);
                if (value instanceof Component) {
                    return (Component) value;
                }
            }
        } catch (Throwable t) {
            if (!loggedSettingsReflectionFailure) {
                loggedSettingsReflectionFailure = true;
                ForgeControllerLog.logException("Failed to create literal component", t);
            }
        }
        return Component.m_237113_(text);
    }

    public static Button createButton(int x, int y, int w, int h, String label, Button.OnPress onPress) {
        Button.Builder builder = Button.m_253074_(textLiteral(label), onPress);
        builder.m_252987_(x, y, w, h);
        return builder.m_253136_();
    }
}
