package banditvault.xboxcompat;

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



public final class XboxCompatLog {
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.dir", "."), "xbox_compat.log");
    private static final OpenOption[] OPEN_OPTIONS = new OpenOption[] {
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND
    };

    private XboxCompatLog() {
    }

    public static synchronized void log(String message) {
        String line = "[" + LocalTime.now() + "] [xbox_compat] " + message + System.lineSeparator();
        try (OutputStream out = Files.newOutputStream(LOG_PATH, OPEN_OPTIONS)) {
            out.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Diagnostics must never interfere with game startup.
        }
    }


    public static void probeNetwork() {
        log("network probe start");
        try {
            var ctx = javax.net.ssl.SSLContext.getDefault();
            log("SSLContext default ok: " + ctx.getProtocol());
        } catch (Throwable t) {
            logException("SSLContext init failed", t);
        }
        try (var s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("textures.minecraft.net", 443), 5000);
            log("tcp connect ok");
        } catch (Throwable t) {
            logException("tcp connect failed", t);
        }
        try {
            var url = java.net.URI.create("https://textures.minecraft.net/").toURL();
            var c = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            log("https response: " + c.getResponseCode());
        } catch (Throwable t) {
            logException("https failed", t);
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
        log(buffer.toString().stripTrailing());
    }
}
package banditvault.xboxcompat;

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

public final class XboxCompatLog {
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.dir", "."), "xbox_compat.log");
    private static final OpenOption[] OPEN_OPTIONS = new OpenOption[] {
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.APPEND
    };

    private XboxCompatLog() {
    }

    public static synchronized void log(String message) {
        String line = "[" + LocalTime.now() + "] [xbox_compat] " + message + System.lineSeparator();
        try (OutputStream out = Files.newOutputStream(LOG_PATH, OPEN_OPTIONS)) {
            out.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Diagnostics must never interfere with game startup.
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
        log(buffer.toString().stripTrailing());
    }
}
