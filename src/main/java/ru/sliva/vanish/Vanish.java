package ru.sliva.vanish;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Vanish extends JavaPlugin implements Runnable, Listener {

    private final List<UUID> vanishList = new ArrayList<>();
    private int updateTask;
    private TeleportCommand teleportCommand;

    public List<UUID> getVanishList() {
        return vanishList;
    }

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        updateTask = Bukkit.getScheduler().runTaskTimer(this, this, 20, 0).getTaskId();
        //Credits to the Bukkit Organization
        this.teleportCommand = new TeleportCommand(this);
    }

    @Override
    public void onDisable() {
        if (Bukkit.getScheduler().isCurrentlyRunning(updateTask)) {
            Bukkit.getScheduler().cancelTask(updateTask);
        }
        vanishList.clear();
        run();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "vanish":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (this.vanishList.contains(player.getUniqueId())) {
                        this.vanishList.remove(player.getUniqueId());
                        player.sendMessage(Component.text("Невидимость выключена для Вас.").color(NamedTextColor.RED));
                    } else {
                        this.vanishList.add(player.getUniqueId());
                        player.sendMessage(Component.text("Невидимость включена для Вас.").color(NamedTextColor.GREEN));
                    }
                }
                break;
            case "tp":
                return teleportCommand.onCommand(sender, command, label, args);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if(command.getName().equalsIgnoreCase("tp")) {
            return teleportCommand.onTabComplete(sender, command, alias, args);
        }
        return Collections.emptyList();
    }

    @Override
    public void run() {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (vanishList.contains(p.getUniqueId())) {
                    Bukkit.getOnlinePlayers().forEach(target -> target.hidePlayer(this, p));
                } else {
                    Bukkit.getOnlinePlayers().forEach(target -> target.showPlayer(this, p));
                }
            }
        } else {
            Bukkit.getScheduler().cancelTask(updateTask);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void ping(PaperServerListPingEvent e) {
        List<PlayerProfile> profiles = e.getPlayerSample();
        for (UUID id : vanishList) {
            OfflinePlayer f = Bukkit.getOfflinePlayer(id);
            if (f.isOnline()) {
                Player p = (Player) f;
                e.setNumPlayers(e.getNumPlayers() - 1);
                profiles.remove(p.getPlayerProfile());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void join(PlayerJoinEvent e) {
        if (Bukkit.getOnlinePlayers().size() > 0 && !Bukkit.getScheduler().isCurrentlyRunning(updateTask)) {
            updateTask = Bukkit.getScheduler().runTaskTimer(this, this, 20, 0).getTaskId();
        }
        Player p = e.getPlayer();
        if (vanishList.contains(p.getUniqueId())) {
            e.setJoinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void quit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (vanishList.contains(p.getUniqueId())) {
            e.setQuitMessage(null);
        }
    }
}