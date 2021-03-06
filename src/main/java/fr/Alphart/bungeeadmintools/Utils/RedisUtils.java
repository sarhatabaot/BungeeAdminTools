package fr.alphart.bungeeadmintools.utils;

import java.util.UUID;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;


public class RedisUtils implements Listener {

    private boolean redis = false;
    private static String channel = "BungeeAdminTools";
    private static String split = "######";

    public RedisUtils(final boolean enable) {
        if (enable) {
            if (BungeeAdminToolsPlugin.getInstance().getProxy().getPluginManager().getPlugin("RedisBungee") != null && RedisBungee.getApi() != null) {
                BungeeAdminToolsPlugin.getInstance().getLogger().info("Detected RedisBungee.  Enabling experimental RedisBungee support.  This currently only supports RedisBungee 0.3.3 or higher (but not 0.4).");
                BungeeAdminToolsPlugin.getInstance().getProxy().getPluginManager()
                        .registerListener(BungeeAdminToolsPlugin.getInstance(), this);
                RedisBungee.getApi().registerPubSubChannels(channel);
                redis = true;
            } else {
                redis = false;
            }
        } else {
            redis = false;
        }
    }

    @EventHandler
    public void onPubSubMessage(final PubSubMessageEvent e) {
        if (!e.getChannel().equals(channel)) return;

        String[] message = e.getMessage().split(split);

        if (message[0].equalsIgnoreCase(RedisBungee.getApi().getServerId()) || message.length < 3) return;

        String messageType = message[1];

        switch (messageType) {
            case "gkick" -> recieveGKickPlayer(message[2], message[3]);
            case "message" -> recieveMessagePlayer(message[2], message[3]);
            case "broadcast" -> recieveBroadcast(message[2], message[3]);
            case "muteupdate" -> recieveMuteUpdatePlayer(message[2], message[3]);
            case "movedefaultserver" -> recieveMoveDefaultServerPlayer(message[2]);
            default -> BungeeAdminToolsPlugin.getInstance().getLogger().warning("Undeclared BungeeAdminTool redis message recieved: " + messageType);
        }
    }

    public Boolean isRedisEnabled() {
        return redis;
    }

    public void sendMessagePlayer(UUID pUUID, String message) {
        if (!redis) return;
        sendMessage("message", pUUID.toString() + split + message);
    }

    private void recieveMessagePlayer(String sUUID, String message) {
        ProxiedPlayer player = BungeeAdminToolsPlugin.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }

    public void sendGKickPlayer(UUID pUUID, String reason) {
        if (!redis) return;
        sendMessage("gkick", pUUID.toString() + split + reason);
    }

    private void recieveGKickPlayer(String sUUID, String reason) {
        if (BungeeAdminToolsPlugin.getInstance().getModules().isLoaded("ban") || BungeeAdminToolsPlugin.getInstance().getModules().isLoaded("kick")) {
            ProxiedPlayer player = BungeeAdminToolsPlugin.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
            if (player != null) {
                BungeeAdminToolsPlugin.kick(player, reason);
            }
        } else {
            throw new IllegalStateException("Neither the ban nor the kick module are enabled. The gkick message can't be handled.");
        }
    }

    public void sendBroadcast(String permission, String broadcast) {
        if (!redis) return;
        sendMessage("broadcast", permission + split + broadcast);
    }

    private void recieveBroadcast(String permission, String broadcast) {
        BungeeAdminToolsPlugin.noRedisBroadcast(broadcast, permission);
    }

    public void sendMuteUpdatePlayer(UUID pUUID, String server) {
        if (!redis) return;
        sendMessage("muteupdate", pUUID.toString() + split + server);
    }

    private void recieveMuteUpdatePlayer(String sUUID, String server) {
        if (BungeeAdminToolsPlugin.getInstance().getModules().isLoaded("mute")) {
            ProxiedPlayer player = BungeeAdminToolsPlugin.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
            if (player != null) {
                try {
                    BungeeAdminToolsPlugin.getInstance().getModules().getMuteModule().updateMuteData(player.getName());
                } catch (InvalidModuleException ignored) {
                }
            }
        } else {
            throw new IllegalStateException("The mute module isn't enabled. The mute message can't be handled.");
        }
    }

    public void sendMoveDefaultServerPlayer(UUID pUUID) {
        if (!redis) return;
        sendMessage("movedefaultserver", pUUID.toString());
    }

    private void recieveMoveDefaultServerPlayer(String sUUID) {
        ProxiedPlayer player = BungeeAdminToolsPlugin.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
        if (player != null) {
            player.connect(ProxyServer.getInstance().getServerInfo(
                    player.getPendingConnection().getListener().getDefaultServer()));
        }
    }

    void sendMessage(String messageType, String messageBody) {
        if (!redis)
            return;

        if (messageBody.trim().length() == 0)
            return;

        final String message = RedisBungee.getApi().getServerId() + split + messageType + split + messageBody;

        BungeeAdminToolsPlugin.getInstance().getProxy().getScheduler().runAsync(BungeeAdminToolsPlugin.getInstance(), () -> RedisBungee.getApi().sendChannelMessage(channel, message));
    }

    public void destroy() {
        if (!redis) return;
        RedisBungee.getApi().unregisterPubSubChannels("BungeeAdminTools");
        BungeeAdminToolsPlugin.getInstance().getProxy().getPluginManager()
                .unregisterListener(this);
    }

}
