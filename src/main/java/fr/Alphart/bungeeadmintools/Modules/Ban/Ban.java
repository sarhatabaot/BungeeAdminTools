package fr.alphart.bungeeadmintools.modules.ban;

import static fr.alphart.bungeeadmintools.i18n.I18n.formatWithColor;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


import co.aikar.commands.BaseCommand;
import fr.alphart.bungeeadmintools.i18n.I18n;
import fr.alphart.bungeeadmintools.modules.ModuleConfiguration;
import fr.alphart.bungeeadmintools.modules.core.Core;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import com.google.common.base.Charsets;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.utils.FormatUtils;
import fr.alphart.bungeeadmintools.utils.UuidNotFoundException;
import fr.alphart.bungeeadmintools.utils.Utils;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;

public class Ban implements IModule, Listener {
    private final String name = "ban";
    private ScheduledTask task;
    private BanCommand banCommand;
    private final BanConfig config;

    public Ban() {
        config = new BanConfig();
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public ModuleConfiguration getConfig() {
        return config;
    }

    @Override
    public BaseCommand getCommand() {
        return banCommand;
    }

    @Override
    public boolean load() {
        // Init table
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            final Statement statement = conn.createStatement();
            statement.executeUpdate(SQLQueries.Ban.createTable);
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        }

        // Register commands
        banCommand = new BanCommand(this);
        //commandHandler = new OldBanCommand(this);
        //commandHandler.loadCommands();

        // Launch tempban task
        final BanExpirationTask banExpirationTask = new BanExpirationTask(this);
        task = ProxyServer.getInstance().getScheduler().schedule(BungeeAdminToolsPlugin.getInstance(), banExpirationTask, 0, 10, TimeUnit.SECONDS);

        // Check if the online players are banned (if the module has been reloaded)
        for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            final List<String> serversToCheck = player.getServer() != null
                    ? Arrays.asList(player.getServer().getInfo().getName(), GLOBAL_SERVER)
                    : Collections.singletonList(GLOBAL_SERVER);
            for (final String server : serversToCheck) {
                if (isBan(player, server)) {
                    if (server.equals(player.getPendingConnection().getListener().getServerPriority().get(0)) || server.equals(GLOBAL_SERVER)) {
                        player.disconnect(getBanMessage(player.getPendingConnection(), server));
                        continue;
                    }
                    player.sendMessage(getBanMessage(player.getPendingConnection(), server));
                    player.connect(ProxyServer.getInstance().getServerInfo(player.getPendingConnection().getListener().getServerPriority().get(0)));
                }
            }
        }

        return true;
    }

    @Override
    public boolean unload() {
        task.cancel();
        return true;
    }

    public class BanConfig extends ModuleConfiguration {
        public BanConfig() {
            init(name);
        }
    }

    public BaseComponent[] getBanMessage(final PendingConnection pConn, final String server) {
        String reason = "";
        Timestamp expiration = null;
        Timestamp begin = null;
        String staff = null;

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Ban.getBanMessage);
            try {
                final UUID pUUID;
                if (pConn.getUniqueId() != null) {
                    pUUID = pConn.getUniqueId();
                } else {
                    pUUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pConn.getName()).getBytes(StandardCharsets.UTF_8));
                }
                statement.setString(1, pUUID.toString().replace("-", ""));
                statement.setString(2, pConn.getAddress().getAddress().getHostAddress());
                statement.setString(3, server);
            } catch (final UuidNotFoundException e) {
                BungeeAdminToolsPlugin.getInstance().getLogger().severe("Error during retrieving of the UUID of " + pConn.getName() + ". Please report this error :");
                e.printStackTrace();
            }
            resultSet = statement.executeQuery();

            if (resultSet.next()) {

                begin = resultSet.getTimestamp("ban_begin");
                expiration = resultSet.getTimestamp("ban_end");

                reason = (resultSet.getString("ban_reason") != null) ? resultSet.getString("ban_reason") : IModule.NO_REASON;
                staff = resultSet.getString("ban_staff");
            } else {
                throw new SQLException("No active ban found.");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        if (expiration != null) {
            return TextComponent.fromLegacyText(I18n.formatWithColor("isBannedTemp",
                    new String[]{reason, (expiration.getTime() < System.currentTimeMillis()) ? "a few moments" : FormatUtils.getDuration(expiration.getTime()),
                            Core.defaultDF.format(begin), staff}));
        } else {
            return TextComponent.fromLegacyText(I18n.formatWithColor("isBanned", new String[]{reason, Core.defaultDF.format(begin), staff}));
        }
    }

    /**
     * Check if both ip and name of this player are banned
     *
     * @param player
     * @param server
     * @return true if name or ip is banned
     */
    public boolean isBan(final ProxiedPlayer player, final String server) {
        final String ip = Core.getPlayerIP(player.getName());
        return isBan(player.getName(), server) || isBan(ip, server);
    }

    public boolean isBan(final ProxiedPlayer player) {
        return isBan(player, GLOBAL_SERVER);
    }

    /**
     * Check if this entity (player or ip) is banned
     *
     * @param bannedEntity | can be an ip or a player name
     * @param server       | if server equals to (any) check if the player is ban on a
     *                     server
     * @return
     */
    public boolean isBan(final String bannedEntity, final String server) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            // If this is an ip which may be banned
            if (Utils.validIP(bannedEntity)) {
                statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBanIP
                        : SQLQueries.Ban.isBanServerIP);
                statement.setString(1, bannedEntity);
                if (!ANY_SERVER.equals(server)) {
                    statement.setString(2, server);
                }
            }
            // If this is a player which may be banned
            else {
                final String uuid = Core.getUuid(bannedEntity);
                statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBan
                        : SQLQueries.Ban.isBanServer);
                statement.setString(1, uuid);
                if (!ANY_SERVER.equals(server)) {
                    statement.setString(2, server);
                }
            }
            resultSet = statement.executeQuery();

            // If there are a result
            if (resultSet.next()) {
                return true;
            }

        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return false;
    }

    public String banIp(final String ip, final String server, final String staff, final long expirationTimestamp, final String reason) {
        return ban(ip, server, staff, expirationTimestamp, reason);
    }

    /**
     * Ban this entity (player or ip) <br>
     *
     * @param bannedEntity        | can be an ip or a player name
     * @param server              ; set to "(global)", to global ban
     * @param staff
     * @param expirationTimestamp ; set to 0 for ban def
     * @param reason              | optional
     * @return
     */
    public String ban(final String bannedEntity, final String server, final String staff,
                      final long expirationTimestamp, final String reason) {
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            // If the bannedEntity is an ip
            if (Utils.validIP(bannedEntity)) {
                return banPlayerIP(conn, bannedEntity, staff, server, expirationTimestamp, reason);
            }

            return banPlayer(conn, bannedEntity, staff, server, expirationTimestamp, reason);
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        }
    }

    private String banPlayer(final Connection conn, final String bannedEntity, final String staff, final String server, final long expirationTimestamp, final String reason) throws SQLException {
        final String sUUID = Core.getUuid(bannedEntity);
        final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(bannedEntity);
        final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBan);
        statement.setString(1, sUUID);
        statement.setString(2, staff);
        statement.setString(3, server);
        statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
        statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
        statement.executeUpdate();
        statement.close();

        // Kick player if he's online and on the server where he's
        // banned
        if (player != null
                && (server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equalsIgnoreCase(server))) {
            BungeeAdminToolsPlugin.kick(player, I18n.formatWithColor("wasBannedNotif", new String[]{reason}));
        }

        if (expirationTimestamp > 0) {
            return I18n.formatWithColor("banTempBroadcast", new String[]{bannedEntity, FormatUtils.getDuration(expirationTimestamp),
                    staff, server, reason});
        } else {
            return I18n.formatWithColor("banBroadcast", new String[]{bannedEntity, staff, server, reason});
        }
    }

    private String banPlayerIP(final Connection conn, final String bannedEntity, final String staff, final String server, final long expirationTimestamp, final String reason) throws SQLException {
        final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBanIP);
        statement.setString(1, bannedEntity);
        statement.setString(2, staff);
        statement.setString(3, server);
        statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
        statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
        statement.executeUpdate();
        statement.close();

        for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (Utils.getPlayerIP(player).equals(bannedEntity) && (GLOBAL_SERVER.equals(server) || server.equalsIgnoreCase(player.getServer().getInfo().getName()))) {
                BungeeAdminToolsPlugin.kick(player, I18n.formatWithColor("wasBannedNotif", new String[]{reason}));
            }
        }


        if (expirationTimestamp > 0) {
            return I18n.formatWithColor("banTempBroadcast", new String[]{bannedEntity, FormatUtils.getDuration(expirationTimestamp),
                    staff, server, reason});
        }
        return I18n.formatWithColor("banBroadcast", new String[]{bannedEntity, staff, server, reason});
    }

    /**
     * Ban the ip of an online player
     *
     * @param server              ; set to "(global)", to global ban
     * @param staff
     * @param expirationTimestamp duration
     *                            ; set to 0 for ban def
     * @param reason              | optional
     * @param player
     */
    public String banIP(final ProxiedPlayer player, final String server, final String staff,
                        final long expirationTimestamp, final String reason) {
        ban(Utils.getPlayerIP(player), server, staff, expirationTimestamp, reason);
        return I18n.formatWithColor("banBroadcast", new String[]{player.getName() + "'s IP", staff, server, reason});
    }


    /**
     * Unban an entity (player or ip)
     *
     * @param bannedEntity | can be an ip or a player name
     * @param server       | if equals to (any), unban from all servers | if equals to
     *                     (global), remove global ban
     * @param staff
     * @param reason
     */
    public String unBan(final String bannedEntity, final String server, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            // If the bannedEntity is an ip
            if (Utils.validIP(bannedEntity)) {
                if (ANY_SERVER.equals(server)) {
                    statement = (conn.prepareStatement(SQLQueries.Ban.unBanIP));
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, bannedEntity);
                } else {
                    statement = (conn
                            .prepareStatement(SQLQueries.Ban.unBanIPServer));
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, bannedEntity);
                    statement.setString(4, server);
                }

            }

            // Otherwise it's a player
            else {
                final String UUID = Core.getUuid(bannedEntity);
                if (ANY_SERVER.equals(server)) {
                    statement = (conn.prepareStatement(SQLQueries.Ban.unBan));
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, UUID);
                } else {
                    statement = (conn.prepareStatement(SQLQueries.Ban.unBanServer));
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, UUID);
                    statement.setString(4, server);
                }

            }
            statement.executeUpdate();
            return I18n.formatWithColor("unbanBroadcast", new String[]{bannedEntity, staff, server, reason});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

    }

    /**
     * Unban the ip of this entity
     *
     * @param entity
     * @param server | if equals to (any), unban from all servers | if equals to
     *               (global), remove global ban
     * @param staff
     * @param reason | optional
     */
    public String unBanIP(final String entity, final String server, final String staff, final String reason) {
        if (Utils.validIP(entity)) {
            return unBan(entity, server, staff, reason);
        } else {
            unBan(Core.getPlayerIP(entity), server, staff, reason);
            return I18n.formatWithColor("unbanBroadcast", new String[]{entity + "'s IP", staff, server, reason});
        }
    }

    /**
     * Get all ban data of an entity <br>
     * <b>Should be runned async to optimize performance</b>
     *
     * @param entity
     * @return List of BanEntry of the player
     */
    public List<BanEntry> getBanData(final String entity) {
        final List<BanEntry> banList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            // If the entity is an ip
            if (Utils.validIP(entity)) {
                statement = conn.prepareStatement(SQLQueries.Ban.getBanIP);
                statement.setString(1, entity);
                resultSet = statement.executeQuery();
            }
            // Otherwise if it's a player
            else {
                statement = conn.prepareStatement(SQLQueries.Ban.getBan);
                statement.setString(1, Core.getUuid(entity));
                resultSet = statement.executeQuery();
            }

            while (resultSet.next()) {
                final Timestamp beginDate;
                final Timestamp endDate;
                final Timestamp unbanDate;

                beginDate = resultSet.getTimestamp("ban_begin");
                endDate = resultSet.getTimestamp("ban_end");
                unbanDate = resultSet.getTimestamp("ban_unbandate");


                // Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
                final String server = resultSet.getString("ban_server");
                String reason = resultSet.getString("ban_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                final String staff = resultSet.getString("ban_staff");
                final boolean active = (resultSet.getBoolean("ban_state"));
                String unbanReason = resultSet.getString("ban_unbanreason");
                if (unbanReason == null) {
                    unbanReason = NO_REASON;
                }
                final String unbanStaff = resultSet.getString("ban_unbanstaff");
                banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return banList;
    }

    public List<BanEntry> getManagedBan(final String staff) {
        final List<BanEntry> banList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Ban.getManagedBan);
            statement.setString(1, staff);
            statement.setString(2, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final Timestamp beginDate;
                final Timestamp endDate;
                final Timestamp unbanDate;

                beginDate = resultSet.getTimestamp("ban_begin");
                endDate = resultSet.getTimestamp("ban_end");
                unbanDate = resultSet.getTimestamp("ban_unbandate");


                // Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
                final String server = resultSet.getString("ban_server");
                String reason = resultSet.getString("ban_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                String entity = (resultSet.getString("ban_ip") != null)
                        ? resultSet.getString("ban_ip")
                        : Core.getPlayerName(resultSet.getString("UUID"));
                // If the UUID search failed
                if (entity == null) {
                    entity = "UUID:" + resultSet.getString("UUID");
                }
                final boolean active = (resultSet.getBoolean("ban_state"));
                String unbanReason = resultSet.getString("ban_unbanreason");
                if (unbanReason == null) {
                    unbanReason = NO_REASON;
                }
                final String unbanStaff = resultSet.getString("ban_unbanstaff");
                banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return banList;
    }

    // Event listener

    @EventHandler
    public void onServerConnect(final ServerConnectEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final String target = e.getTarget().getName();

        if (isBan(player, target)) {
            if (target.equals(player.getPendingConnection().getListener().getDefaultServer())) {
                // If it's player's join server kick him
                if (e.getPlayer().getServer() == null) {
                    e.setCancelled(true);
                    // Need to delay for avoiding the "bit cannot be cast to fm exception" and to annoy the banned player :p
                    ProxyServer.getInstance().getScheduler().schedule(BungeeAdminToolsPlugin.getInstance(), () -> e.getPlayer().disconnect(getBanMessage(player.getPendingConnection(), target)), 500, TimeUnit.MILLISECONDS);
                } else {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(getBanMessage(player.getPendingConnection(), target));
                }
                return;
            }
            player.sendMessage(getBanMessage(player.getPendingConnection(), target));
            if (player.getServer() == null) {
                player.connect(ProxyServer.getInstance().getServerInfo(
                        player.getPendingConnection().getListener().getDefaultServer()));
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLogin(final LoginEvent ev) {
        ev.registerIntent(BungeeAdminToolsPlugin.getInstance());
        BungeeAdminToolsPlugin.getInstance().getProxy().getScheduler().runAsync(BungeeAdminToolsPlugin.getInstance(), () -> {
            boolean isBanPlayer = false;

            PreparedStatement statement = null;
            ResultSet resultSet = null;
            UUID uuid;
            try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
                statement = conn.prepareStatement("SELECT ban_id FROM `BAT_ban` WHERE ban_state = 1 AND UUID = ? AND ban_server = '" + GLOBAL_SERVER + "';");
                // If this is an online mode server, the uuid will be already set
                if (ev.getConnection().getUniqueId() != null) {
                    uuid = ev.getConnection().getUniqueId();
                }
                // Otherwise it's an offline mode server, so we're gonna generate the UUID using player name (hashing)
                else {
                    uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + ev.getConnection().getName()).getBytes(Charsets.UTF_8));
                }
                statement.setString(1, uuid.toString().replaceAll("-", ""));

                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    isBanPlayer = true;
                }
            } catch (SQLException e) {
                DataSourceHandler.handleException(e);
            } finally {
                DataSourceHandler.close(statement, resultSet);
            }

            if ((isBanPlayer) || (isBan(ev.getConnection().getAddress().getAddress().getHostAddress(), GLOBAL_SERVER))) {
                BaseComponent[] bM = getBanMessage(ev.getConnection(), GLOBAL_SERVER);
                ev.setCancelReason(TextComponent.toLegacyText(bM));
                ev.setCancelled(true);
            }
            ev.completeIntent(BungeeAdminToolsPlugin.getInstance());
        });
    }
}