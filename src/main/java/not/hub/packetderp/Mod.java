package not.hub.packetderp;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public final class Mod extends JavaPlugin implements Listener {

    private final HashMap<UUID, String> kickUuids = new HashMap<>();
    private final HashMap<UUID, String> banUuids = new HashMap<>();

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.getInstance().values()) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Server.KEEP_ALIVE)
                        && (kickUuids.containsKey(event.getPlayer().getUniqueId()) || banUuids.containsKey(event.getPlayer().getUniqueId()))) {
                    event.setCancelled(true);
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Client.getInstance().values()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Client.KEEP_ALIVE)
                        && (kickUuids.containsKey(event.getPlayer().getUniqueId()) || banUuids.containsKey(event.getPlayer().getUniqueId()))) {
                    event.setCancelled(true);
                }
            }
        });

    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {

        if (!commandSender.isOp()) {
            return false;
        }

        if (command.getLabel().equalsIgnoreCase("ghostlist")) {
            String message = "ghost kicks: "
                    + kickUuids.values().stream().sorted().collect(Collectors.joining(", "))
                    + System.lineSeparator()
                    + "ghost bans: "
                    + banUuids.values().stream().sorted().collect(Collectors.joining(", "));
            logAndSendMessage(message, commandSender);
            return true;
        }

        if (args.length == 0) {
            commandSender.sendMessage("Please specify a valid target!");
            return false;
        }

        final String userInputLowerCase = args[0].toLowerCase();

        Player onlinePlayer = getServer()
                .getOnlinePlayers()
                .stream()
                .filter(player -> player.getName().toLowerCase().equals(userInputLowerCase))
                .findFirst()
                .orElse(null);

        if (command.getLabel().equalsIgnoreCase("ghostkick")) {
            if (onlinePlayer == null) {
                commandSender.sendMessage("Please specify a valid target!");
                return false;
            }
            kickUuids.put(onlinePlayer.getUniqueId(), onlinePlayer.getName());
            logAndSendMessage("Ghost kicking: " + onlinePlayer.getName(), commandSender);
            return true;
        }

        if (command.getLabel().equalsIgnoreCase("ghostban") || command.getLabel().equalsIgnoreCase("ghostunban")) {

            UUID uuid;
            String name;

            if (onlinePlayer == null) {
                try {
                    uuid = UuidFinder.getByName(userInputLowerCase);
                    name = userInputLowerCase;
                } catch (ExecutionException | UuidFinder.PlayerNotFoundException e) {
                    commandSender.sendMessage("Unable to find UUID for name: " + userInputLowerCase);
                    getLogger().info("Unable to find UUID for name: " + userInputLowerCase + " -> " + e.getMessage());
                    return false;
                }
            } else {
                uuid = onlinePlayer.getUniqueId();
                name = onlinePlayer.getName();
            }

            if (command.getLabel().equalsIgnoreCase("ghostban")) {
                banUuids.put(uuid, name);
                logAndSendMessage("Added ghostban for: " + name, commandSender);
            } else {
                if (banUuids.remove(uuid) != null) {
                    logAndSendMessage("Removed ghostban for: " + name, commandSender);
                } else {
                    commandSender.sendMessage("No existing ghostban for: " + name);
                }
            }

            return true;

        }

        return false;

    }

    private void logAndSendMessage(String message, CommandSender commandSender) {
        getLogger().info(message);
        if (commandSender instanceof Player) {
            commandSender.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerLoginEvent event) {
        if (kickUuids.remove(event.getPlayer().getUniqueId()) != null) {
            getLogger().info("Removed ghostkick for: " + event.getPlayer().getName());
        }
        if (banUuids.containsKey(event.getPlayer().getUniqueId())) {
            getLogger().info("Ghostban active for: " + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onPlayerLeaveEvent(PlayerQuitEvent event) {
        if (kickUuids.remove(event.getPlayer().getUniqueId()) != null) {
            getLogger().info("Removed ghostkick for: " + event.getPlayer().getName());
        }
    }

}
