package banditvault.forgecontroller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;

public final class ForgeControllerLog {
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.dir", "."), "xbox_compat.log");
    private static final OpenOption[] OPEN_OPTIONS = new OpenOption[] {
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND
    };

    private ForgeControllerLog() {
    }

    public static synchronized void log(String message) {
        String line = "[" + LocalTime.now() + "] [forge_controller] " + message + System.lineSeparator();
        try (OutputStream out = Files.newOutputStream(LOG_PATH, OPEN_OPTIONS)) {
            out.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Diagnostics should never break gameplay.
        }
    }

    public static void logException(String message, Throwable throwable) {
        StringWriter buffer = new StringWriter();
        try (PrintWriter writer = new PrintWriter(buffer)) {
            writer.println(message);
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
        }
        log(trimTrailing(buffer.toString()));
    }

    private static String trimTrailing(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
