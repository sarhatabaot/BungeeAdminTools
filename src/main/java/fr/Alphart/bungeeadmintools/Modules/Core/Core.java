package fr.alphart.bungeeadmintools.modules.core;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import co.aikar.commands.BaseCommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.ModuleConfiguration;
import fr.alphart.bungeeadmintools.utils.EnhancedDateFormat;
import fr.alphart.bungeeadmintools.utils.MojangAPIProvider;
import fr.alphart.bungeeadmintools.utils.UuidNotFoundException;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nullable;


public class Core implements IModule, Listener {
    private static final LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public String load(final String pName) throws UuidNotFoundException {
                            final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
                            if (player != null) {
                                // Note: if it's an offline server, the UUID will be generated using
                                // this
                                // function java.util.UUID.nameUUIDFromBytes, however it's an
                                // prenium or cracked account
                                // Online server : bungee handle great the UUID
                                return player.getUniqueId().toString().replaceAll("-", "");
                            }

                            PreparedStatement statement = null;
                            ResultSet resultSet = null;
                            String uuid = "";
                            // Try to get the UUID from the BAT db
                            try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
                                statement = conn.prepareStatement(SQLQueries.Core.getUUID);
                                statement.setString(1, pName);
                                resultSet = statement.executeQuery();
                                if (resultSet.next()) {
                                    uuid = resultSet.getString("UUID");
                                }
                            } catch (final SQLException e) {
                                DataSourceHandler.handleException(e);
                            } finally {
                                DataSourceHandler.close(statement, resultSet);
                            }

                            // If online server, retrieve the UUID from the mojang server
                            if (uuid.isEmpty() && ProxyServer.getInstance().getConfig().isOnlineMode()) {
                                uuid = MojangAPIProvider.getUUID(pName);
                                if (uuid == null) {
                                    throw new UuidNotFoundException(pName);
                                }
                            }
                            // If offline server, generate the UUID
                            else if (uuid.isEmpty()) {
                                uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pName).getBytes(StandardCharsets.UTF_8)).toString().replaceAll("-", "");
                            }

                            return uuid;
                        }
                    });
    private BaseCommand coreCommand;
    public static EnhancedDateFormat defaultDF = new EnhancedDateFormat(false);

    @Override
    public String getName() {
        return "core";
    }

    @Override
    public ModuleConfiguration getConfig() {
        return null;
    }

    @Override
    public boolean load() {
        // Init players table
        Statement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.createStatement();
            statement.executeUpdate(SQLQueries.Core.createTable);
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        coreCommand = new CoreCommand(BungeeAdminToolsPlugin.getInstance());

        // Update the date format (if translation has been changed)
        Core.defaultDF = new EnhancedDateFormat(BungeeAdminToolsPlugin.getInstance().getConfiguration().isLitteralDate());

        return true;
    }

    @Override
    public boolean unload() {

        return true;
    }
    @Override
    public BaseCommand getCommand() {
        return coreCommand;
    }



    /**
     * Get the UUID of the specified player
     *
     * @param pName
     * @return String which is the UUID
     * @throws UuidNotFoundException
     */
    @Nullable
    public static String getUuid(final String pName) throws UuidNotFoundException {
        try {
            return uuidCache.get(pName);
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof UuidNotFoundException) {
                throw (UuidNotFoundException) e.getCause();
            }
        }
        return null;
    }

    /**
     * Convert an string uuid into an UUID object
     *
     * @param strUUID
     * @return UUID
     */
    public static UUID getUUIDfromString(final String strUUID) {
        final String dashesUUID = strUUID.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
        return UUID.fromString(dashesUUID);
    }

    /**
     * Get the player name from a UUID using the BAT database
     *
     * @param UUID
     * @return player name with this UUID or "unknowName"
     */
    public static String getPlayerName(final String UUID) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Core.getPlayerName);
            statement.setString(1, UUID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("BAT_player");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return null;
    }

    /**
     * Update the IP and UUID of a player in the database
     *
     * @param player
     */
    public void updatePlayerIPandUUID(final ProxiedPlayer player) {
        PreparedStatement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            final String ip = Utils.getPlayerIP(player);
            final String UUID = player.getUniqueId().toString().replaceAll("-", "");
            statement = (conn.prepareStatement(SQLQueries.Core.updateIPUUID));
            statement.setString(1, player.getName());
            statement.setString(2, ip);
            statement.setString(3, UUID);
            statement.setString(4, ip);
            statement.setString(5, player.getName());
            statement.executeUpdate();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

    }

    public static String getPlayerIP(final String pName) {

            final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
            if (player != null) return Utils.getPlayerIP(player);


        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Core.getIP);
            statement.setString(1, getUuid(pName));
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("lastip");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return "0.0.0.0";
    }

    /**
     * Get the command sender permission list using bungee api or bungeeperms api if it installed
     *
     * @param sender
     * @return permission in a collection of strings
     */
    public static Collection<String> getCommandSenderPermission(final CommandSender sender) {
        return sender.getPermissions();
    }


    // Event listener
    @EventHandler
    public void onPlayerJoin(final PostLoginEvent ev) {
        BungeeAdminToolsPlugin.getInstance().getProxy().getScheduler().runAsync(BungeeAdminToolsPlugin.getInstance(), () -> updatePlayerIPandUUID(ev.getPlayer()));
    }

    @EventHandler
    public void onPlayerLeft(final PlayerDisconnectEvent ev) {
        CommandQueue.clearQueuedCommand(ev.getPlayer());
    }
}