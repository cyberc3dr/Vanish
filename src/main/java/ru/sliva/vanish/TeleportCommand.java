package ru.sliva.vanish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TeleportCommand implements TabExecutor {

    private static final int MAX_COORD = 30000000;
    private static final int MIN_COORD_MINUS_ONE = -30000001;
    private static final int MIN_COORD = -30000000;

    private final List<UUID> vanishList;

    public TeleportCommand(@NotNull Vanish plugin) {
        this.vanishList = plugin.getVanishList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission("minecraft.command.teleport")) {
            sender.sendMessage(Bukkit.getPermissionMessage());
            return true;
        }

        if (args.length < 1 || args.length > 4) {
            sender.sendMessage(Component.text("Использование: " + command.getUsage()).color(NamedTextColor.RED));
            return true;
        }

        String playerName = args[0];

        Player player;

        if (args.length == 1 || args.length == 3) {
            // Исполнитель пытается телепортировать себя
            if (sender instanceof Player) {
                // Присваиваем исполнителя как телепортируемого
                player = (Player) sender;
            } else {
                // Если консоль - останавливаем команду
                sender.sendMessage(Component.text("Введите имя игрока первым аргументом").color(NamedTextColor.RED));
                return true;
            }
        } else {
            // Получаем игрока из первого аргумента
            player = Bukkit.getPlayerExact(playerName);
        }

        if (player == null) {
            sender.sendMessage(Component.text("Игрок " + playerName + " не найден").color(NamedTextColor.RED));
            return true;
        }

        // -------------------

        if (args.length < 3) {
            // Указан игрок ищем его
            String targetName = args[args.length - 1];

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(Component.text("Не могу найти игрока " + targetName).color(NamedTextColor.RED));
                return true;
            }
            player.teleport(target, PlayerTeleportEvent.TeleportCause.COMMAND);
            if(!vanishList.contains(player.getUniqueId())) {
                broadcastCommandMessage(sender, player.displayName().append(Component.text(" телепортирован к ")).append(target.displayName()));
            }
        } else {
            // Указаны координаты
            Location playerLocation = player.getLocation();
            double x = getCoordinate(playerLocation.getX(), args[args.length - 3]);
            double y = getCoordinate(playerLocation.getY(), args[args.length - 2], 0, 0);
            double z = getCoordinate(playerLocation.getZ(), args[args.length - 1]);

            if (x == MIN_COORD_MINUS_ONE || y == MIN_COORD_MINUS_ONE || z == MIN_COORD_MINUS_ONE) {
                sender.sendMessage(Component.text("Предоставьте действительные координаты!").color(NamedTextColor.RED));
                return true;
            }

            playerLocation.setX(x);
            playerLocation.setY(y);
            playerLocation.setZ(z);

            player.teleport(playerLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
            if(!vanishList.contains(player.getUniqueId())) {
                broadcastCommandMessage(sender, player.displayName().append(Component.text(" телепортирован в точку " + x + " " + y + " " + z)));
            }
        }
        return true;
    }

    private double getCoordinate(double current, String input) {
        return getCoordinate(current, input, MIN_COORD, MAX_COORD);
    }

    private double getCoordinate(double current, String input, int min, int max) {
        boolean relative = input.startsWith("~");
        double result = relative ? current : 0;

        if (!relative || input.length() > 1) {
            boolean exact = input.contains(".");
            if (relative) input = input.substring(1);

            double testResult = getDouble(input);
            if (testResult == MIN_COORD_MINUS_ONE) {
                return MIN_COORD_MINUS_ONE;
            }
            result += testResult;

            if (!exact && !relative) result += 0.5f;
        }
        if (min != 0 || max != 0) {
            if (result < min) {
                result = MIN_COORD_MINUS_ONE;
            }

            if (result > max) {
                result = MIN_COORD_MINUS_ONE;
            }
        }

        return result;
    }

    private double getDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ex) {
            return MIN_COORD_MINUS_ONE;
        }
    }

    private void broadcastCommandMessage(@NotNull CommandSender source, @NotNull Component message) {
        broadcastCommandMessage(source, message, true);
    }

    private void broadcastCommandMessage(@NotNull CommandSender source, @NotNull Component message, boolean sendToSource) {
        Component result = getDisplayName(source).append(Component.text(": ")).append(message);

        if (source instanceof BlockCommandSender) {
            BlockCommandSender blockCommandSender = (BlockCommandSender) source;

            if (Objects.equals(Boolean.FALSE, blockCommandSender.getBlock().getWorld().getGameRuleValue(GameRule.COMMAND_BLOCK_OUTPUT))) {
                Bukkit.getConsoleSender().sendMessage(result);
                return;
            }
        } else if (source instanceof CommandMinecart) {
            CommandMinecart commandMinecart = (CommandMinecart) source;

            if (Objects.equals(Boolean.FALSE, commandMinecart.getWorld().getGameRuleValue(GameRule.COMMAND_BLOCK_OUTPUT))) {
                Bukkit.getConsoleSender().sendMessage(result);
                return;
            }
        }

        Set<Permissible> users = Bukkit.getPluginManager().getPermissionSubscriptions(Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
        Component formatted = Component.text("[").append(result).append(Component.text("]")).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.TRUE);

        if (sendToSource && !(source instanceof ConsoleCommandSender)) {
            source.sendMessage(message);
        }

        for (Permissible user : users) {
            if (user instanceof CommandSender && user.hasPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE)) {
                CommandSender target = (CommandSender) user;

                if (target instanceof ConsoleCommandSender) {
                    target.sendMessage(result);
                } else if (target != source) {
                    target.sendMessage(formatted);
                }
            }
        }
    }

    private Component getDisplayName(@NotNull CommandSender sender) {
        if(sender instanceof Player) {
            return ((Player) sender).displayName();
        }
        return Component.text(sender.getName());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if(sender.hasPermission("minecraft.command.teleport")) {
            if(args.length <= 2) {
                return null;
            }
        }
        return Collections.emptyList();
    }
}
