package ru.sliva.vanish

import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

object Vanish : JavaPlugin(), Listener {

    val vanishList: MutableList<UUID> = ArrayList()

    val onlinePlayers: List<Player>
        get() = vanishList.mapNotNull { Bukkit.getPlayer(it) }

    override fun onEnable() {
        Bukkit.getServer().pluginManager.registerEvents(this, this)
        Bukkit.getServicesManager().register(VanishService.javaClass, VanishService, this, ServicePriority.Highest)
    }

    override fun onDisable() {
        vanishList.clear()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            if (vanishList.contains(sender.uniqueId)) {
                vanishList.remove(sender.uniqueId)
                sender.sendMessage(Component.text("Невидимость выключена для Вас.").color(NamedTextColor.RED))
                Bukkit.getOnlinePlayers().forEach { it.showPlayer(this, sender) }
            } else {
                vanishList.add(sender.uniqueId)
                sender.sendMessage(Component.text("Невидимость включена для Вас.").color(NamedTextColor.GREEN))
                Bukkit.getOnlinePlayers().forEach { it.hidePlayer(this, sender) }
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        return emptyList()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun ping(e: PaperServerListPingEvent) {
        e.playerSample.removeAll(onlinePlayers.map { it.playerProfile })
        e.numPlayers -= onlinePlayers.size
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun join(e: PlayerJoinEvent) {
        val p = e.player
        onlinePlayers.forEach { p.hidePlayer(this, it) }
        if (vanishList.contains(p.uniqueId)) {
            e.joinMessage(null)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun quit(e: PlayerQuitEvent) {
        val p = e.player
        if (vanishList.contains(p.uniqueId)) {
            e.quitMessage(null)
        }
    }
}