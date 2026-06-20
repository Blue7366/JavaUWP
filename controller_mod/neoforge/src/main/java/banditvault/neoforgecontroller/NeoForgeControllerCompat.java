package banditvault.neoforgecontroller;

import banditvault.controllercore.ControllerAxis;
import banditvault.controllercore.ControllerButton;
import banditvault.controllercore.ControllerRuntime;
import banditvault.controllercore.ControllerState;
import com.mojang.blaze3d.platform.InputConstants;
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

public final class NeoForgeControllerCompat {
    private static final int GAMEPAD_ID = GLFW.GLFW_JOYSTICK_1;
    private static final int LEFT_CLICK = 0;
    private static final int RIGHT_CLICK = 1;
    private static final double RELAY_CURSOR_MOVE_EPSILON = 0.75;
    private static final double CONTROLLER_CURSOR_TAKEOVER_THRESHOLD = 0.35;

    private static final GLFWGamepadState GLFW_STATE = GLFWGamepadState.create();
    private static final ControllerState CONTROLLER_STATE = new ControllerState();
    private static final ReflectionCache REFLECTION = new ReflectionCache();

    private static boolean active;
    private static boolean loggedReady;
    private static boolean loggedScreenReflectionFailure;
    private static boolean loggedNoGamepad;
    private static boolean loggedInit;
    private static long tickCount;
    private static boolean crouchToggled;
    private static boolean sprintToggled;
    private static double cursorX = -1.0;
    private static double cursorY = -1.0;
    private static int scrollCooldown;
    private static long lastLookNanos;
    private static long lastScreenCursorNanos;
    private static long renderFrameActiveNanos;
    private static boolean loggedLookApplied;
    private static Object lastCursorScreen;
    private static Object lastRelayCursorScreen;
    private static double lastRelayCursorX = Double.NaN;
    private static double lastRelayCursorY = Double.NaN;
    private static boolean relayOwnsCursor;

    private NeoForgeControllerCompat() {
    }

    public static void markRenderFrameActive() {
        renderFrameActiveNanos = System.nanoTime();
    }

    public static void ensureInitialized() {
        if (loggedInit) {
            return;
        }
        loggedInit = true;
        NeoForgeControllerLog.log("NeoForge controller compat initialized");
    }

    public static void tick(Minecraft client) {
        ensureInitialized();
        tickCount++;
        if (client == null) {
            return;
        }
        ensureMenuCursorMode(client);

        if (!poll()) {
            if (!loggedNoGamepad || tickCount <= 5 || tickCount % 600 == 0) {
                loggedNoGamepad = true;
                NeoForgeControllerLog.log("No GLFW gamepad detected on joystick 1 (tick=" + tickCount + ")");
            }
            if (active) {
                releaseGameplayKeys(client);
                crouchToggled = false;
                sprintToggled = false;
                active = false;
            }
            return;
        }
        loggedNoGamepad = false;

        active = true;
        if (!loggedReady) {
            loggedReady = true;
            NeoForgeControllerLog.log("NeoForge controller compat active");
        }

        if (client.screen != null) {
            lastLookNanos = 0L;
            releaseGameplayKeys(client);
            tickScreen(client, client.screen);
        } else {
            tickGameplay(client);
        }

        finishFrame();
    }

    public static void renderFrame(Minecraft client) {
        ensureMenuCursorMode(client);
        if (client == null || client.screen != null || client.player == null || !poll()) {
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

        NeoForgeControllerSettings settings = NeoForgeControllerSettings.get();
        float y = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
        if (settings.invertY) {
            y = -y;
        }
        applyLook(client.player, axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X), y, seconds, settings);
    }

    public static void renderCursor(Screen screen, GuiGraphics graphics) {
        if (!active || screen == null || graphics == null) {
            return;
        }
        if (relayOwnsCursor || cursorX < 0.0 || cursorY < 0.0) {
            return;
        }
        int x = (int) Math.round(cursorX);
        int y = (int) Math.round(cursorY);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, 1000.0);
        graphics.fill(x - 3, y - 3, x + 4, y + 4, 0x66000000);
        graphics.fill(x - 5, y, x + 6, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y - 5, x + 1, y + 6, 0xFFFFFFFF);
        graphics.pose().popPose();
    }

    public static boolean shouldRenderCursorInBaseScreen(Screen screen) {
        return !(screen instanceof AbstractContainerScreen);
    }

    public static void updateScreenCursorBeforeRender(Screen screen, int mouseX, int mouseY) {
        ensureMenuCursorMode(Minecraft.getInstance());
        if (!active || screen == null || Minecraft.getInstance() == null || Minecraft.getInstance().screen != screen) {
            return;
        }
        observeRelayCursor(screen);
        if (relayOwnsCursor) {
            invokeScreenMouseMoved(screen, lastRelayCursorX, lastRelayCursorY);
        }
        updateScreenCursor(Minecraft.getInstance(), screen, true);
    }

    public static int screenMouseX(int fallback) {
        if (!active || relayOwnsCursor || cursorX < 0.0) {
            return fallback;
        }
        return (int) Math.round(cursorX);
    }

    public static int screenMouseY(int fallback) {
        if (!active || relayOwnsCursor || cursorY < 0.0) {
            return fallback;
        }
        return (int) Math.round(cursorY);
    }

    private static boolean poll() {
        try {
            if (!GLFW.glfwJoystickIsGamepad(GAMEPAD_ID) ||
                !GLFW.glfwGetGamepadState(GAMEPAD_ID, GLFW_STATE)) {
                return false;
            }
            captureControllerState();
            return true;
        } catch (RuntimeException t) {
            if (!loggedReady) {
                loggedReady = true;
                NeoForgeControllerLog.logException("NeoForge controller compat failed to poll GLFW gamepad", t);
            }
            return false;
        }
    }

    private static void ensureMenuCursorMode(Minecraft client) {
        if (client == null || client.screen == null || client.getWindow() == null) {
            return;
        }
        long window = client.getWindow().getWindow();
        if (window != 0L && GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    private static void tickGameplay(Minecraft client) {
        NeoForgeControllerSettings settings = NeoForgeControllerSettings.get();
        Options options = client.options;
        if (options == null) {
            return;
        }
        NeoForgeControllerKeys keys = new NeoForgeControllerKeys(options);

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_BACK)) {
            client.setScreen(new NeoForgeControllerSettingsScreen(null));
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
        sneakHeld = setHeld(keys.sneak, sneakHeld);
        setSneakInput(client.player, sneakHeld);

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
            changeHotbarSlot(client.player, -1);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)) {
            changeHotbarSlot(client.player, 1);
        }
        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_START)) {
            client.pauseGame(false);
        }

        if (!renderFramePathActive()) {
            float y = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
            if (settings.invertY) {
                y = -y;
            }
            applyLook(client.player, axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X), y, 1.0f / 20.0f, settings);
        }
    }

    private static void tickScreen(Minecraft client, Screen screen) {
        NeoForgeControllerSettings settings = NeoForgeControllerSettings.get();
        ensureScreenCursor(screen);
        float ry = axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);

        if (pressed(GLFW.GLFW_GAMEPAD_BUTTON_BACK)) {
            if (screen instanceof NeoForgeControllerSettingsScreen) {
                ((NeoForgeControllerSettingsScreen) screen).close();
            } else {
                client.setScreen(new NeoForgeControllerSettingsScreen(screen));
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
                client.setScreen(null);
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

    private static void ensureScreenCursor(Screen screen) {
        if (screen != lastCursorScreen) {
            lastCursorScreen = screen;
            lastScreenCursorNanos = 0L;
            cursorX = Math.max(1, screen.width / 2);
            cursorY = Math.max(1, screen.height / 2);
            return;
        }
        if (cursorX < 0.0 || cursorY < 0.0) {
            cursorX = Math.max(1, screen.width / 2);
            cursorY = Math.max(1, screen.height / 2);
        }
    }

    private static void updateScreenCursor(Minecraft client, Screen screen, boolean frameTimed) {
        if (client == null || screen == null || !poll()) {
            return;
        }
        ensureScreenCursor(screen);
        NeoForgeControllerSettings settings = NeoForgeControllerSettings.get();
        float rawX = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        float rawY = axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        if (relayOwnsCursor) {
            double takeoverMagnitude = Math.max(Math.abs(rawX), Math.abs(rawY));
            if (takeoverMagnitude < CONTROLLER_CURSOR_TAKEOVER_THRESHOLD) {
                lastScreenCursorNanos = System.nanoTime();
                return;
            }
            relayOwnsCursor = false;
        }
        double dx = shapedCursorAxis(rawX, settings.cursorDeadzone);
        double dy = shapedCursorAxis(rawY, settings.cursorDeadzone);

        double scale = settings.cursorSpeed;
        if (frameTimed) {
            long now = System.nanoTime();
            double seconds = 1.0 / 60.0;
            if (lastScreenCursorNanos != 0L) {
                seconds = (now - lastScreenCursorNanos) / 1000000000.0;
                if (seconds < 0.0) {
                    seconds = 0.0;
                } else if (seconds > 1.0 / 20.0) {
                    seconds = 1.0 / 20.0;
                }
            }
            lastScreenCursorNanos = now;
            scale *= seconds * 30.0;
        }

        if (dx != 0.0 || dy != 0.0) {
            cursorX = clamp(cursorX + dx * scale, 0.0, Math.max(1, screen.width - 1));
            cursorY = clamp(cursorY + dy * scale, 0.0, Math.max(1, screen.height - 1));
            invokeScreenMouseMoved(screen, cursorX, cursorY);
        }
    }

    private static void observeRelayCursor(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.mouseHandler == null) {
            return;
        }
        double mouseX = client.mouseHandler.xpos();
        double mouseY = client.mouseHandler.ypos();
        if (client.getWindow() != null) {
            mouseX = client.mouseHandler.xpos() * screen.width / Math.max(1.0, client.getWindow().getScreenWidth());
            mouseY = client.mouseHandler.ypos() * screen.height / Math.max(1.0, client.getWindow().getScreenHeight());
        }
        if (screen != lastRelayCursorScreen || Double.isNaN(lastRelayCursorX) || Double.isNaN(lastRelayCursorY)) {
            lastRelayCursorScreen = screen;
            lastRelayCursorX = mouseX;
            lastRelayCursorY = mouseY;
            return;
        }

        if (Math.abs(mouseX - lastRelayCursorX) > RELAY_CURSOR_MOVE_EPSILON ||
            Math.abs(mouseY - lastRelayCursorY) > RELAY_CURSOR_MOVE_EPSILON) {
            relayOwnsCursor = true;
        }
        lastRelayCursorX = mouseX;
        lastRelayCursorY = mouseY;
    }

    private static void applyLook(LocalPlayer player, float rx, float ry, float seconds, NeoForgeControllerSettings settings) {
        float lookX = ControllerRuntime.shapedLookAxis(rx, settings.lookDeadzone);
        float lookY = ControllerRuntime.shapedLookAxis(ry, settings.lookDeadzone);
        if (lookX == 0.0f && lookY == 0.0f) {
            return;
        }
        float scale = settings.lookSpeed * seconds;
        player.setYRot(player.getYRot() + lookX * scale);
        player.setXRot((float) clamp(player.getXRot() + lookY * scale, -90.0, 90.0));
        if (!loggedLookApplied) {
            loggedLookApplied = true;
            NeoForgeControllerLog.log(String.format(
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
        Options options = client.options;
        if (options == null) {
            return;
        }
        NeoForgeControllerKeys keys = new NeoForgeControllerKeys(options);
        setHeld(keys.forward, false);
        setHeld(keys.back, false);
        setHeld(keys.left, false);
        setHeld(keys.right, false);
        setHeld(keys.jump, false);
        boolean sneakHeld = setHeld(keys.sneak, false);
        setSneakInput(client.player, sneakHeld);
        setHeld(keys.sprint, false);
        setHeld(keys.attack, false);
        setHeld(keys.use, false);
    }

    private static boolean setHeld(KeyMapping key, boolean held) {
        if (key == null) {
            return false;
        }
        InputConstants.Key inputKey = key.getKey();
        boolean effectiveHeld = held || isBoundInputHeld(inputKey);
        key.setDown(effectiveHeld);
        if (inputKey != null) {
            KeyMapping.set(inputKey, effectiveHeld);
        }
        return effectiveHeld;
    }

    private static boolean isBoundInputHeld(InputConstants.Key inputKey) {
        Minecraft client = Minecraft.getInstance();
        if (inputKey == null || client == null || client.getWindow() == null) {
            return false;
        }
        long window = client.getWindow().getWindow();
        int code = inputKey.getValue();
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS
            || (code >= 0 && code <= GLFW.GLFW_MOUSE_BUTTON_LAST
                && GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS);
    }

    private static void setSneakInput(LocalPlayer player, boolean sneak) {
        if (player == null || player.input == null) {
            return;
        }
        player.input.shiftKeyDown = sneak;
    }

    private static void pressKey(KeyMapping key) {
        if (key == null) {
            return;
        }
        InputConstants.Key inputKey = key.getKey();
        if (inputKey != null) {
            KeyMapping.click(inputKey);
        }
    }

    private static void changeHotbarSlot(LocalPlayer player, int direction) {
        if (player == null) {
            return;
        }
        int slot = (player.getInventory().selected + direction) % 9;
        if (slot < 0) {
            slot += 9;
        }
        player.getInventory().selected = slot;
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
            if (slot == null || !slot.hasItem()) {
                return;
            }
            REFLECTION.slotClickedMethod.invoke(screen, slot, slot.index, 0, ClickType.QUICK_MOVE);
        } catch (ReflectiveOperationException t) {
            if (!loggedScreenReflectionFailure) {
                loggedScreenReflectionFailure = true;
                NeoForgeControllerLog.logException("NeoForge controller compat failed to quick-move focused slot", t);
            }
        }
    }

    private static void invokeScreenMouseMoved(Screen screen, double x, double y) {
        screen.mouseMoved(x, y);
    }

    private static void invokeScreenMousePressed(Screen screen, double x, double y, int button) {
        screen.mouseClicked(x, y, button);
    }

    private static void invokeScreenMouseReleased(Screen screen, double x, double y, int button) {
        screen.mouseReleased(x, y, button);
    }

    private static void invokeScreenMouseScrolled(Screen screen, double x, double y, double scroll) {
        screen.mouseScrolled(x, y, 0.0, scroll);
    }

    private static boolean invokeScreenKeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        return screen.keyPressed(keyCode, scanCode, modifiers);
    }

    private static float axis(int index) {
        return CONTROLLER_STATE.axis(axisFor(index));
    }

    private static boolean trigger(int index) {
        return CONTROLLER_STATE.trigger(axisFor(index), NeoForgeControllerSettings.get().triggerDeadzone);
    }

    private static boolean triggerPressed(int index) {
        return CONTROLLER_STATE.triggerPressed(axisFor(index), NeoForgeControllerSettings.get().triggerDeadzone);
    }

    private static boolean button(int index) {
        return CONTROLLER_STATE.button(buttonFor(index));
    }

    private static boolean pressed(int index) {
        return CONTROLLER_STATE.pressed(buttonFor(index));
    }

    private static boolean released(int index) {
        return CONTROLLER_STATE.released(buttonFor(index));
    }

    private static double shapedCursorAxis(float value, float deadzone) {
        return ControllerRuntime.shapedCursorAxis(value, deadzone);
    }

    private static double clamp(double value, double min, double max) {
        return ControllerRuntime.clamp(value, min, max);
    }

    private static void captureControllerState() {
        float[] axes = new float[] {
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X),
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y),
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X),
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y),
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER),
            GLFW_STATE.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER)
        };
        boolean[] buttons = new boolean[15];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = GLFW_STATE.buttons(i) == GLFW.GLFW_PRESS;
        }
        CONTROLLER_STATE.capture(axes, buttons);
    }

    private static void finishFrame() {
        CONTROLLER_STATE.finishFrame(NeoForgeControllerSettings.get().triggerDeadzone);
    }

    private static ControllerAxis axisFor(int index) {
        switch (index) {
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_X: return ControllerAxis.LEFT_X;
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y: return ControllerAxis.LEFT_Y;
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X: return ControllerAxis.RIGHT_X;
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y: return ControllerAxis.RIGHT_Y;
            case GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER: return ControllerAxis.LEFT_TRIGGER;
            case GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER: return ControllerAxis.RIGHT_TRIGGER;
            default: return ControllerAxis.LEFT_X;
        }
    }

    private static ControllerButton buttonFor(int index) {
        switch (index) {
            case GLFW.GLFW_GAMEPAD_BUTTON_A: return ControllerButton.A;
            case GLFW.GLFW_GAMEPAD_BUTTON_B: return ControllerButton.B;
            case GLFW.GLFW_GAMEPAD_BUTTON_X: return ControllerButton.X;
            case GLFW.GLFW_GAMEPAD_BUTTON_Y: return ControllerButton.Y;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER: return ControllerButton.LEFT_BUMPER;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER: return ControllerButton.RIGHT_BUMPER;
            case GLFW.GLFW_GAMEPAD_BUTTON_BACK: return ControllerButton.BACK;
            case GLFW.GLFW_GAMEPAD_BUTTON_START: return ControllerButton.START;
            case GLFW.GLFW_GAMEPAD_BUTTON_GUIDE: return ControllerButton.GUIDE;
            case GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB: return ControllerButton.LEFT_THUMB;
            case GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB: return ControllerButton.RIGHT_THUMB;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP: return ControllerButton.DPAD_UP;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT: return ControllerButton.DPAD_RIGHT;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN: return ControllerButton.DPAD_DOWN;
            case GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT: return ControllerButton.DPAD_LEFT;
            default: return ControllerButton.A;
        }
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

        private final Field hoveredSlotField = findField(AbstractContainerScreen.class, "hoveredSlot");
        private final Method slotClickedMethod = findMethod(AbstractContainerScreen.class, "slotClicked",
            Slot.class, int.class, int.class, ClickType.class);

        private ReflectionCache() {
            logMissingBindingsOnce();
        }

        private void logMissingBindingsOnce() {
            if (loggedMissingBindings) {
                return;
            }
            loggedMissingBindings = true;
            if (hoveredSlotField == null || slotClickedMethod == null) {
                NeoForgeControllerLog.log("NeoForge controller inventory bindings incomplete; quick move may be limited");
            }
        }
    }

    public static Component textLiteral(String text) {
        return Component.literal(text);
    }

    public static Button createButton(int x, int y, int w, int h, String label, Button.OnPress onPress) {
        return Button.builder(textLiteral(label), onPress).bounds(x, y, w, h).build();
    }
}
