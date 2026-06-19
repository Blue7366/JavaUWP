package banditvault.fabriccontroller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;

public final class FabricControllerLog {
    private static final File LOG_FILE = new File(System.getProperty("user.dir", "."), "xbox_compat.log");

    private FabricControllerLog() {
    }

    public static void log(String message) {
        String line = "[" + LocalTime.now() + "] [fabric_controller] " + message + System.lineSeparator();
        try (FileWriter out = new FileWriter(LOG_FILE, true)) {
            out.write(line);
        } catch (IOException ignored) {
        }
    }

    public static void logException(String message, Throwable t) {
        log(message + ": " + t);
    }
}
