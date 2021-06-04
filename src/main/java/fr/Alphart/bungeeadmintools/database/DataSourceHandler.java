package fr.alphart.bungeeadmintools.database;


import com.google.common.base.Preconditions;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.zaxxer.hikari.HikariDataSource;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;

public class DataSourceHandler {
	// Connection informations
	private final HikariDataSource ds;
	private final String username;
	private final String password;
	private final String database;
	private final String port;
	private final String host;

	private Connection SQLiteConn;

	/**
	 * Constructor used for MySQL
	 * 
	 * @param host
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @throws SQLException 
	 */
	public DataSourceHandler(final String host, final String port, final String database, final String username, final String password) throws SQLException{
		// Check database's informations and init connection
		this.host = Preconditions.checkNotNull(host);
		this.port = Preconditions.checkNotNull(port);
		this.database = Preconditions.checkNotNull(database);
		this.username = Preconditions.checkNotNull(username);
		this.password = Preconditions.checkNotNull(password);

		BungeeAdminToolsPlugin.getInstance().getLogger().config("Initialization of HikariCP in progress ...");
		BasicConfigurator.configure(new NullAppender());
		ds = new HikariDataSource();
		ds.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + 
				"?useLegacyDatetimeCode=false&serverTimezone=" + TimeZone.getDefault().getID());
		ds.setUsername(this.username);
		ds.setPassword(this.password);
		ds.addDataSourceProperty("cachePrepStmts", "true");
		ds.setMaximumPoolSize(8);
		try {
			final Connection conn = ds.getConnection();
		    int intOffset = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
		    String offset = String.format("%02d:%02d", Math.abs(intOffset / 3600), Math.abs((intOffset / 60) % 60));
		    offset = (intOffset >= 0 ? "+" : "-") + offset;
			conn.createStatement().executeQuery("SET time_zone='" + offset + "';");
			conn.close();
			BungeeAdminToolsPlugin.getInstance().getLogger().config("BoneCP is loaded !");
		} catch (final SQLException e) {
			BungeeAdminToolsPlugin.getInstance().getLogger().severe("BAT encounters a problem during the initialization of the database connection."
					+ " Please check your logins and database configuration.");
			if(e.getCause() instanceof CommunicationsException){
			    BungeeAdminToolsPlugin.getInstance().getLogger().severe(e.getCause().getMessage());
			}
			if(BungeeAdminToolsPlugin.getInstance().getConfiguration().isDebugMode()){
			    BungeeAdminToolsPlugin.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
			}
			throw e;
		}
	}


	public Connection getConnection() {
		try {
			return ds.getConnection();
		} catch (final SQLException e) {
			BungeeAdminToolsPlugin.getInstance().getLogger().severe(
			        "BAT can't etablish connection with the database. Please report this and include the following lines :");
			if(e.getCause() instanceof CommunicationsException){
			    BungeeAdminToolsPlugin.getInstance().getLogger().severe(e.getCause().getMessage());
			}
            if (BungeeAdminToolsPlugin.getInstance().getConfiguration().isDebugMode()) {
                e.printStackTrace();
            }
			return null;
		}
	}



	// Useful methods
	public static String handleException(final SQLException e) {
		BungeeAdminToolsPlugin.getInstance()
		.getLogger()
		.severe("BAT encounters a problem with the database. Please report this and include the following lines :");
		e.printStackTrace();
		return "An error related to the database occured. Please check the log.";
	}

	public static void close(final AutoCloseable... closableList) {
		for (final AutoCloseable closable : closableList) {
			if (closable != null) {
				try {
					closable.close();
				} catch (final Throwable ignored) {
				}
			}
		}
	}

}