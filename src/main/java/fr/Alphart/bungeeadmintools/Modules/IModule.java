package fr.alphart.bungeeadmintools.modules;

import co.aikar.commands.BaseCommand;

public interface IModule {
	// Constants
	// Server groups related
	String NO_REASON = "noreason";
	String GLOBAL_SERVER = "(global)";
	String ANY_SERVER = "(any)";

	// Module part
	Integer ON_STATE = 1;
	Integer OFF_STATE = 0;

	String getName();

	/**
	 * Load the module
	 * 
	 * @return true if everything's ok otherwise false
	 */
	boolean load();

	/**
	 * Get the configuration section of this module
	 * 
	 * @return configuration section of this module
	 */
	ModuleConfiguration getConfig();

	/**
	 * Unload the module
	 * 
	 * @return true if everything's ok otherwise false
	 */
	boolean unload();

	BaseCommand getCommand();
}