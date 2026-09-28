package io.spiritsakura;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Extracts the bundled pack, optionally serves it over HTTP, and sends it to players. */
public final class PackHost {
    private final JavaPlugin plugin;
    private HttpServer server;
    private ExecutorService executor;
    private byte[] sha1;
    private byte[] data;
    private String url;

    public PackHost(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() throws Exception {
        FileConfiguration cfg = plugin.getConfig();

        String theme = cfg.getString("pack.theme", Themes.DEFAULT).toLowerCase();
        if (!Themes.exists(theme)) {
            plugin.getLogger().warning("Unknown theme '" + theme + "', using " + Themes.DEFAULT);
            theme = Themes.DEFAULT;
        }
        String resource = "packs/" + theme + ".zip";

        // Always refresh the on-disk copy so plugin updates ship a matching pack.
        Path file = plugin.getDataFolder().toPath().resolve(resource);
        Files.createDirectories(file.getParent());
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) throw new IOException("Bundled " + resource + " is missing from the plugin jar");
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
        data = Files.readAllBytes(file);
        sha1 = MessageDigest.getInstance("SHA-1").digest(data);

        String external = cfg.getString("pack.external-url", "").trim();
        if (!external.isEmpty()) {
            url = external.replace("{theme}", theme);
            plugin.getLogger().info("Using external resource pack URL: " + url);
            return;
        }

        if (!cfg.getBoolean("pack.host.enabled", true)) {
            plugin.getLogger().warning("Pack hosting is disabled and no external-url is set; the pack will not be sent.");
            return;
        }

        String bind = cfg.getString("pack.host.bind", "0.0.0.0");
        int port = cfg.getInt("pack.host.port", 8163);
        String publicAddress = cfg.getString("pack.host.public-address", "127.0.0.1");

        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/pack.zip", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        });
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SpiritSakura-PackHost");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.start();

        url = "http://" + publicAddress + ":" + port + "/pack.zip";
        plugin.getLogger().info("Hosting resource pack at " + url + " (make sure TCP " + port + " is reachable)");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public boolean isAvailable() {
        return url != null && sha1 != null;
    }

    public String url() {
        return url;
    }

    public void send(Player player) {
        if (!isAvailable()) return;
        FileConfiguration cfg = plugin.getConfig();
        Component prompt = MiniMessage.miniMessage().deserialize(cfg.getString("pack.prompt", "Spirit Sakura"));
        // Deterministic ID: clients only re-download when the pack contents change.
        UUID id = UUID.nameUUIDFromBytes(sha1);
        player.setResourcePack(id, url, sha1, prompt, cfg.getBoolean("pack.force", false));
    }
}
