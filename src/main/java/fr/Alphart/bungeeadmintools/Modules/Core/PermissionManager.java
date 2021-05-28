package fr.alphart.bungeeadmintools.modules.core;

import fr.alphart.bungeeadmintools.modules.IModule;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PermissionManager {
    private static final String PERM_PREFIX = "bat.";

    public enum Action {
        BAN("ban"),
        BANIP("banip"),
        TEMPBAN("tempban"),
        TEMPBANIP("tempbanip"),
        UNBAN("unban"),
        UNBANIP("unbanip"),
        BAN_BROADCAST("ban.broadcast"),

        MUTE("mute"),
        MUTEIP("muteip"),
        TEMPMUTE("tempmute"),
        TEMPMUTEIP("tempmuteip"),
        UNMUTE("unmute"),
        UNMUTEIP("unmuteip"),
        MUTE_BROADCAST("mute.broadcast"),

        KICK("kick"),
        WARN("warn"),
        WARN_BROADCAST("warn.broadcast"),
        KICK_BROADCAST("kick.broadcast"),

        LOOKUP("lookup");

        String permission;

        Action(final String permission) {
            this.permission = permission;
        }

        public String getPermission() {
            return PERM_PREFIX + permission;
        }

        @Override
        public String toString() {
            return getPermission();
        }

        public String getText() {
            return this.permission;
        }

        public static Action fromText(String text) {
            if (text != null) {
                for (Action b : Action.values()) {
                    if (text.equalsIgnoreCase(b.permission)) {
                        return b;
                    }
                }
            }
            return null;
        }

    }

    /**
     * Check if the command sender can exec this action on this server
     *
     * @param action
     * @param executor
     * @param server
     * @return true if he can otherwise false
     */
    public static boolean canExecuteAction(final Action action, final CommandSender executor, final String server) {
        if (executor.hasPermission("bat.admin") || executor.hasPermission(PERM_PREFIX + "grantall.global")) {
            return true;
        }
        // If the user has global perm, check if he has some perm which negates this
        if (executor.hasPermission(PERM_PREFIX + "grantall." + server) ||
                ((executor.hasPermission(action.getPermission() + ".global") && // If it's for global server (or any which is the same, don't need to check the permission)
                        !(server.equals(IModule.GLOBAL_SERVER) || server.equals(IModule.ANY_SERVER))))) {

            final String denialPerm = '-' + action.getPermission() + '.' + server;

            for (final String perm : Core.getCommandSenderPermission(executor)) {
                if (perm.equals(denialPerm)) {
                    return false;
                }
            }
            return true;
        }
        // Else just check if he has the specified server perm
        return executor.hasPermission(action.getPermission() + '.' + server);
    }

    /**
     * Check if this entity is an online player (if it's a player) and then
     * return if it is exempt of the specified action
     *
     * @param action
     * @param target
     * @return true if it is exempt from this action otherwise false
     */
    public static boolean isExemptFrom(final Action action, final String target) {
        final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(target);
        if (player != null) {
            return (!player.hasPermission("bat.admin") && !player.hasPermission(action.getPermission() + ".exempt"));
        }
        return true;
    }
}