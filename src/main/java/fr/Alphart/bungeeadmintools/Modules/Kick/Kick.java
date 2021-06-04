package fr.alphart.bungeeadmintools.modules.kick;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import co.aikar.commands.BaseCommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.i18n.I18n;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.ModuleConfiguration;
import fr.alphart.bungeeadmintools.modules.core.Core;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;


public class Kick implements IModule {
	private final String name = "kick";
	private final KickConfig config;
	private BaseCommand kickCommand;

	public Kick(){
		config = new KickConfig();
	}


	@Override
	public BaseCommand getCommand() {
		return kickCommand;
	}

	@Override
	public String getMainCommand() {
		return "kick";
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
	public boolean load() {
		// Init table
		Statement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.createStatement();
			statement.executeUpdate(SQLQueries.Kick.createTable);
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Register commands
		kickCommand = new KickCommand(this);
		//commandHandler = new OldKickCommand(this);
		//commandHandler.loadCommands();

		return true;
	}

	@Override
	public boolean unload() {

		return false;
	}

	public class KickConfig extends ModuleConfiguration {
		public KickConfig() {
			init(name);
		}
	}

	/**
	 * Kick a player and tp him to the default server
	 * 
	 * @param player
	 * @param reason
	 */
	public String kick(final ProxiedPlayer player, final String staff, final String reason) {
		player.connect(ProxyServer.getInstance().getServerInfo(
				player.getPendingConnection().getListener().getDefaultServer()));
		player.sendMessage(TextComponent.fromLegacyText(I18n.formatWithColor("wasKickedNotif", new String[] { reason })));
		return kickSQL(player.getUniqueId(), player.getServer().getInfo().getName(), staff, reason);
	}
	public String kickSQL(final UUID pUUID, final String server, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
			statement.setString(1, pUUID.toString().replace("-", ""));
			statement.setString(2, staff);
			statement.setString(3, reason);
			statement.setString(4, server);
			statement.executeUpdate();
			statement.close();

			return I18n.formatWithColor("kickBroadcast", new String[] { Core.getPlayerName(pUUID.toString().replace("-", "")), staff, server, reason });
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Kick a player from the network
	 * 
	 * @param player
	 * @param reason
	 */
	public String gKick(final ProxiedPlayer player, final String staff, final String reason) {
		final String message = gKickSQL(player.getUniqueId(), staff, reason);
		player.disconnect(TextComponent.fromLegacyText(I18n.formatWithColor("wasKickedNotif", new String[] { reason })));
		return message;
	}
	public String gKickSQL(final UUID pUUID, final String staff, final String reason) {
		PreparedStatement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
			statement.setString(1, pUUID.toString().replace("-", ""));
			statement.setString(2, staff);
			statement.setString(3, reason);
			statement.setString(4, GLOBAL_SERVER);
			statement.executeUpdate();
			statement.close();
			return I18n.formatWithColor("gKickBroadcast", new String[] { BungeeAdminToolsPlugin.getInstance().getProxy().getPlayer(pUUID).getName(), staff, reason });
		} catch (final SQLException e) {
			return DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
	}

	/**
	 * Get all kick data of a player <br>
	 * <b>Should be runned async to optimize performance</b>
	 * 
	 * @param pName
	 *            's name
	 * @return List of KickEntry of the player
	 */
	public List<KickEntry> getKickData(final String pName) {
		final List<KickEntry> kickList = new ArrayList<>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Kick.getKick);
			statement.setString(1, Core.getUUID(pName));
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				final String server = resultSet.getString("kick_server");
				String reason = resultSet.getString("kick_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				final String staff = resultSet.getString("kick_staff");
				final Timestamp date;
				date = resultSet.getTimestamp("kick_date");
				kickList.add(new KickEntry(pName, server, reason, staff, date));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return kickList;
	}
	
	public List<fr.alphart.bungeeadmintools.modules.kick.KickEntry> getManagedKick(final String staff) {
		final List<fr.alphart.bungeeadmintools.modules.kick.KickEntry> kickList = new ArrayList<>();
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.prepareStatement(SQLQueries.Kick.getManagedKick);
			statement.setString(1, staff);
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				final String server = resultSet.getString("kick_server");
				String reason = resultSet.getString("kick_reason");
				if(reason == null){
					reason = NO_REASON;
				}
				String pName = Core.getPlayerName(resultSet.getString("UUID"));
				if(pName == null){
					pName = "UUID:" + resultSet.getString("UUID");
				}
				final Timestamp date;
				date = resultSet.getTimestamp("kick_date");
				kickList.add(new fr.alphart.bungeeadmintools.modules.kick.KickEntry(pName, server, reason, staff, date));
			}
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement, resultSet);
		}
		return kickList;
	}
}