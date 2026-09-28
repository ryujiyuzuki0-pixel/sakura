package io.spiritsakura;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SpiritSakuraPlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final String PERM = "spiritsakura.admin";
    private PackHost packHost;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startPackHost();
        getServer().getPluginManager().registerEvents(this, this);
        var cmd = getCommand("sakura");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        if (packHost != null) packHost.stop();
    }

    private void startPackHost() {
        if (packHost != null) packHost.stop();
        packHost = new PackHost(this);
        try {
            packHost.start();
        } catch (Exception e) {
            getLogger().severe("Could not start resource pack host: " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("pack.send-on-join", true)) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) packHost.send(player);
        }, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage("You don't have permission.");
            return true;
        }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> sender.sendMessage("Spirit items: " +
                    Arrays.stream(SakuraItem.values()).map(SakuraItem::id).collect(Collectors.joining(", ")));
            case "reload" -> {
                reloadConfig();
                startPackHost();
                sender.sendMessage("Reloaded. Pack URL: " + (packHost.isAvailable() ? packHost.url() : "none"));
            }
            case "pack" -> {
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                        : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage("Usage: /sakura pack [player]");
                } else if (!packHost.isAvailable()) {
                    sender.sendMessage("No pack URL available - check config.yml.");
                } else {
                    packHost.send(target);
                    sender.sendMessage("Sent pack to " + target.getName());
                }
            }
            case "themes" -> sender.sendMessage("Themes: " + String.join(", ", Themes.all().keySet()) +
                    " (current: " + currentTheme() + ")");
            case "theme" -> {
                if (args.length < 2 || !Themes.exists(args[1].toLowerCase(Locale.ROOT))) {
                    sender.sendMessage("Usage: /sakura theme <" + String.join("|", Themes.all().keySet()) + ">");
                } else {
                    getConfig().set("pack.theme", args[1].toLowerCase(Locale.ROOT));
                    saveConfig();
                    startPackHost();
                    Bukkit.getOnlinePlayers().forEach(packHost::send);
                    sender.sendMessage("Theme set to " + currentTheme() + " and pack re-sent to online players.");
                }
            }
            case "give" -> give(sender, args);
            default -> sender.sendMessage("Usage: /sakura <give|list|themes|theme|pack|reload>");
        }
        return true;
    }

    private String currentTheme() {
        String t = getConfig().getString("pack.theme", Themes.DEFAULT).toLowerCase(Locale.ROOT);
        return Themes.exists(t) ? t : Themes.DEFAULT;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /sakura give <item|all> [player]");
            return;
        }
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2])
                : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return;
        }
        List<SakuraItem> items = new ArrayList<>();
        if (args[1].equalsIgnoreCase("all")) {
            items.addAll(Arrays.asList(SakuraItem.values()));
        } else {
            SakuraItem item = SakuraItem.byId(args[1]);
            if (item == null) {
                sender.sendMessage("Unknown item. Use /sakura list.");
                return;
            }
            items.add(item);
        }
        for (SakuraItem item : items) {
            ItemStack stack = item.create(Themes.color(currentTheme()));
            target.getInventory().addItem(stack).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
        sender.sendMessage("Gave " + items.size() + " item(s) to " + target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) return List.of();
        if (args.length == 1) return filter(List.of("give", "list", "themes", "theme", "pack", "reload"), args[0]);
        if (args[0].equalsIgnoreCase("give") && args.length == 2) {
            List<String> ids = new ArrayList<>(List.of("all"));
            for (SakuraItem item : SakuraItem.values()) ids.add(item.id());
            return filter(ids, args[1]);
        }
        if (args[0].equalsIgnoreCase("theme") && args.length == 2) {
            return filter(new ArrayList<>(Themes.all().keySet()), args[1]);
        }
        if ((args[0].equalsIgnoreCase("give") && args.length == 3) ||
            (args[0].equalsIgnoreCase("pack") && args.length == 2)) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[args.length - 1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
