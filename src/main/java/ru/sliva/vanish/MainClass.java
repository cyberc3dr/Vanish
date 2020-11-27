package ru.sliva.vanish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;

public class MainClass extends JavaPlugin implements Runnable, Listener{
	
	private PluginManager plMgr;
	private List<UUID> vanishList = new ArrayList<>();
	private int updateTask;
	
	@Override
	public void onEnable() {
		plMgr = Bukkit.getServer().getPluginManager();
		plMgr.registerEvents(this, this);
		updateTask = Bukkit.getScheduler().runTaskTimer(this, this, 20, 0).getTaskId();
	}
	
	@Override
	public void onDisable() {
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("vanish")) {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				if(vanishList.contains(p.getUniqueId())) {
					plMgr.callEvent(new PlayerJoinEvent(p, p.getDisplayName() + "§ejoined the game."));
					p.removePotionEffect(PotionEffectType.INVISIBILITY);
					vanishList.remove(p.getUniqueId());
					p.sendMessage("§aYou are now in vanish mode.");
				} else {
					plMgr.callEvent(new PlayerQuitEvent(p, p.getDisplayName() + "§eleft the game."));
					p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 127));
					vanishList.add(p.getUniqueId());
					p.sendMessage("§cYou are no longer in vanish mode.");
				}
			}
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return Collections.emptyList();
	}

	@Override
	public void run() {
		if(Bukkit.getOnlinePlayers().size() > 0) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(vanishList.contains(p.getUniqueId())) {
					Bukkit.getOnlinePlayers().forEach(target -> target.hidePlayer(this, p));
				} else {
					Bukkit.getOnlinePlayers().forEach(target -> target.showPlayer(this, p));
				}
			}
		} else {
			Bukkit.getScheduler().cancelTask(updateTask);
			System.gc();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void ping(PaperServerListPingEvent e) {
		List<PlayerProfile> profiles = e.getPlayerSample();
		for(UUID id : vanishList) {
			OfflinePlayer f = Bukkit.getOfflinePlayer(id);
			if(f.isOnline()) {
				Player p = (Player) f;
				e.setNumPlayers(e.getNumPlayers() - 1);
				profiles.remove(p.getPlayerProfile());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void join(PlayerJoinEvent e) {
		if(Bukkit.getOnlinePlayers().size() > 0) {
			if(!Bukkit.getScheduler().isCurrentlyRunning(updateTask)) {
				updateTask = Bukkit.getScheduler().runTaskTimer(this, this, 20, 0).getTaskId();
			}
		}
		Player p = e.getPlayer();
		if(vanishList.contains(p.getUniqueId())) {
			e.setJoinMessage(null);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void quit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if(vanishList.contains(p.getUniqueId())) {
			e.setQuitMessage(null);
		}
	}
}