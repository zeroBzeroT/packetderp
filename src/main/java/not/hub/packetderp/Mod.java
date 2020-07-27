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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class Mod extends JavaPlugin implements Listener {

    private final Set<UUID> kickUuids = new HashSet<>();
    private final Set<UUID> banUuids = new HashSet<>();

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.getInstance().values()) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Server.KEEP_ALIVE) && (kickUuids.contains(event.getPlayer().getUniqueId()) || banUuids.contains(event.getPlayer().getUniqueId()))) {
                    event.setCancelled(true);
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Client.getInstance().values()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (!event.getPacketType().equals(PacketType.Play.Client.KEEP_ALIVE) && (kickUuids.contains(event.getPlayer().getUniqueId()) || banUuids.contains(event.getPlayer().getUniqueId()))) {
                    event.setCancelled(true);
                }
            }
        });

    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {

        Player sender;

        if (commandSender instanceof Player) {
            sender = (Player) commandSender;
        } else {
            return false;
        }

        if (!sender.isOp()) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("Please specify a target!");
            return false;
        }

        Player target = getServer().getOnlinePlayers().stream().filter(player -> player.getName().toLowerCase().equals(args[0].toLowerCase())).findFirst().orElse(null);

        if (command.getLabel().equalsIgnoreCase("ghostban")) {

            UUID uuid;
            String name = args[0].toLowerCase();
            if (target == null) {
                try {
                    uuid = MojangApi.getUuidByName(name);
                } catch (ExecutionException e) {
                    sender.sendMessage("Unable to fetch UUID for given name: " + name);
                    getLogger().info("Unable to fetch UUID for given name: " + name + "-" + e.getMessage());
                    return false;
                }
            } else {
                uuid = target.getUniqueId();
                name = target.getName();
            }

            if (!banUuids.remove(uuid)) {
                banUuids.add(uuid);
                sender.sendMessage("Ghost banning: " + name);
                getLogger().info("Ghost banning: " + name);
            } else {
                sender.sendMessage("Removing ghost ban for: " + name);
                getLogger().info("Removing ghost ban for: " + name);
            }
            return true;

        }

        if (target == null) {
            sender.sendMessage("Please specify a target!");
            return false;
        }

        if (command.getLabel().equalsIgnoreCase("ghostkick")) {
            kickUuids.add(target.getUniqueId());
            sender.sendMessage("Ghost kicking: " + target.getName());
            getLogger().info("Ghost kicking: " + target.getName());
            return true;
        }

        return false;

    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerLoginEvent event) {
        if (kickUuids.remove(event.getPlayer().getUniqueId())) {
            getLogger().info("Resuming normal connection for: " + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onPlayerLeaveEvent(PlayerQuitEvent event) {
        if (kickUuids.remove(event.getPlayer().getUniqueId())) {
            getLogger().info("Resuming normal connection for: " + event.getPlayer().getName());
        }
    }

}
