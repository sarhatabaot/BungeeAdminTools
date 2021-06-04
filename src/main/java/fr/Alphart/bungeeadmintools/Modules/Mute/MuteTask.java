package fr.alphart.bungeeadmintools.modules.mute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * This task handles the mute related update :<br>
 * - check in the db for every active mute if it is finished if this is the case
 * : set mute_(ip)state to 0<br>
 * - update the PlayerMuteData of every player on the server <br>
 * <b>This task must be run asynchronously </b>
 */
public class MuteTask implements Runnable {
	private final Mute mute;

	public MuteTask(final Mute muteModule) {
		mute = muteModule;
	}

	@Override
	public void run() {
		Statement statement = null;
		try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
			statement = conn.createStatement();
			statement.executeUpdate(SQLQueries.Mute.updateExpiredMute);
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}
		// Update player mute data
		for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			mute.updateMuteData(player.getName());
		}
	}
}
