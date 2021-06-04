package fr.alphart.bungeeadmintools;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import co.aikar.commands.BungeeCommandManager;
import fr.alphart.bungeeadmintools.modules.ModulesManager;
import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.utils.CallbackUtils;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import fr.alphart.bungeeadmintools.i18n.I18n;

/**
 * Main class BungeeAdminTools
 *
 * @author Alphart
 */
public class BungeeAdminToolsPlugin extends Plugin {
    // This way we can check at runtime if the required BC build (or a higher one) is installed
    private static BungeeAdminToolsPlugin instance;
    private static DataSourceHandler dsHandler;
    private Configuration config;
    private static String prefix;
    private ModulesManager modules;
    @Getter
    private BungeeCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        config = new Configuration();
        getLogger().setLevel(Level.INFO);
        this.commandManager = new BungeeCommandManager(this);
        this.commandManager.enableUnstableAPI("help");
        if (config.isDebugMode()) {
            try {
                final File debugFile = new File(getDataFolder(), "debug.log");
                if (debugFile.exists()) {
                    java.nio.file.Files.delete(debugFile.toPath());
                    //debugFile.delete();
                }
                // Write header into debug log
                Files.asCharSink(debugFile, StandardCharsets.UTF_8).writeLines(Arrays.asList("BAT log debug file"
                                + " - If you have an error with BAT, you should post this file on BAT topic on spigotmc",
                        "Bungee build : " + ProxyServer.getInstance().getVersion(),
                        "BAT version : " + getDescription().getVersion(),
                        "Operating System : " + System.getProperty("os.name"),
                        "Timezone : " + TimeZone.getDefault().getID(),
                        "------------------------------------------------------------"));
                final FileHandler handler = new FileHandler(debugFile.getAbsolutePath(), true);
                handler.setFormatter(new Formatter() {
                    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    private final String pattern = "time [level] message\n";

                    @Override
                    public String format(LogRecord record) {
                        return pattern.replace("level", record.getLevel().getName())
                                .replace("message", record.getMessage())
                                .replace("[BungeeAdminTools]", "")
                                .replace("time", sdf.format(Calendar.getInstance().getTime()));
                    }
                });
                getLogger().addHandler(handler);
                getLogger().setLevel(Level.CONFIG);
                getLogger().info("The debug mode is now enabled ! Log are available in debug.log file located in BAT folder");
                getLogger().config("Debug mode enabled ...");
                getLogger().setUseParentHandlers(false);
            } catch (final Exception e) {
                getLogger().log(Level.SEVERE, "An exception occured during the initialization of debug logging file", e);
            }
        }
        prefix = config.getPrefix();
        loadDB((dbState, throwable) -> {
            if (dbState) {
                getLogger().config("Connection to the database established");
                // Try enabling redis support.
                modules = new ModulesManager();
                modules.loadModules();
            } else {
                getLogger().severe("BAT is gonna shutdown because it can't connect to the database.");
                return;
            }
            // Init the I18n module
            I18n.getString("global");
        });
    }


    @Override
    public void onDisable() {
        modules.unloadModules();
        instance = null;
    }

    public void loadDB(final CallbackUtils.Callback<Boolean> dbState) {

        getLogger().config("Starting connection to the mysql database ...");
        final String username = config.getMysql_user();
        final String password = config.getMysql_password();
        final String database = config.getMysql_database();
        final String port = config.getMysql_port();
        final String host = config.getMysql_host();
        // BoneCP can accept no database and we want to avoid that
        Preconditions.checkArgument(!"".equals(database), "You must set the database.");
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            try {
                dsHandler = new DataSourceHandler(host, port, database, username, password);
                final Connection c = dsHandler.getConnection();
                if (c != null) {
                    c.close();
                    dbState.done(true, null);
                    return;
                }
            } catch (final SQLException handledByDatasourceHandler) {
            }
            getLogger().severe("The connection pool (database connection)"
                    + " wasn't able to be launched !");
            dbState.done(false, null);
        });

    }

    public static BungeeAdminToolsPlugin getInstance() {
        return BungeeAdminToolsPlugin.instance;
    }

    public static BaseComponent[] colorizeAndAddPrefix(final String message) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    /**
     * Send a broadcast message to everyone with the given perm <br>
     * Also broadcast through Redis if it's installed that's why this method <strong>should not be called
     * from a Redis call</strong> otherwise it will broadcast it again and again
     *
     * @param message
     * @param perm
     */
    public static void broadcast(final String message, final String perm) {
        noRedisBroadcast(message, perm);
    }

    public static void noRedisBroadcast(final String message, final String perm) {
        final BaseComponent[] bsMsg = colorizeAndAddPrefix(message);
        for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.hasPermission(perm) || p.hasPermission("bat.admin")) {
                p.sendMessage(bsMsg);
            }
            // If he has a grantall permission, he will have the broadcast on all the servers
            else {
                for (final String playerPerm : Core.getCommandSenderPermission(p)) {
                    if (playerPerm.startsWith("bat.grantall.")) {
                        p.sendMessage(bsMsg);
                        break;
                    }
                }
            }
        }
        getInstance().getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    public ModulesManager getModules() {
        return modules;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public static Connection getConnection() {
        return dsHandler.getConnection();
    }

    public DataSourceHandler getDsHandler() {
        return dsHandler;
    }


    /**
     * Kick a player from the proxy for a specified reason
     *
     * @param player
     * @param reason
     */
    public static void kick(final ProxiedPlayer player, final String reason) {
        if (reason == null || reason.equals("")) {
            player.disconnect(TextComponent.fromLegacyText("You have been disconnected of the server."));
        } else {
            player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', reason)));
        }
    }

}