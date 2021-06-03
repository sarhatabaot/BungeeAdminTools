package fr.alphart.bungeeadmintools.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import fr.alphart.bungeeadmintools.modules.ban.Ban;

import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.modules.kick.Kick;
import fr.alphart.bungeeadmintools.modules.mute.Mute;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.modules.comment.Comment;


public class ModulesManager {
	private final Logger log;
	private String helpMessage;
	private final StringBuilder sb;
	private final Map<IModule, Integer> modules;
	private final Map<String, IModule> modulesNames;

	public ModulesManager() {
		log = BungeeAdminToolsPlugin.getInstance().getLogger();
		sb = new StringBuilder();
		modules = new LinkedHashMap<>();
		modulesNames = new HashMap<>();
	}

	public void showHelp(final CommandSender sender) {
		if (helpMessage == null) {
			sb.append("&2---- &1Bungee&fAdmin&cTools&2 - HELP ----\n");
			for (final Entry<IModule, Integer> entry : modules.entrySet()) {
				if (entry.getValue().equals(IModule.ON_STATE)) {
					sb.append("- &B/");
					sb.append(entry.getKey().getName());
					sb.append(" help&2 : Show the help relative to the ");
					sb.append(entry.getKey().getName());
					sb.append(" module");
				} else {
					sb.append("- &MDisabled : /");
					sb.append(entry.getKey().getName());
					sb.append(" help&2 : Show the help relative to the ");
					sb.append(entry.getKey().getName());
					sb.append(" module");
				}
				sb.append("\n");
			}
			sb.append("&2-----------------------------------------");
			helpMessage = ChatColor.translateAlternateColorCodes('&', sb.toString());
			sb.setLength(0);
		}
		sender.sendMessage(new TextComponent(helpMessage));
	}

	public void loadModules() {
		// The core module MUST NOT be disabled.
		modules.put(new Core(), IModule.OFF_STATE);
		modules.put(new Ban(), IModule.OFF_STATE);
		modules.put(new Mute(), IModule.OFF_STATE);
		modules.put(new Kick(), IModule.OFF_STATE);
		modules.put(new Comment(), IModule.OFF_STATE);
		for (final IModule module : modules.keySet()) {
			// The core doesn't have settings to enable or disable it
			if (!module.getName().equals("core")) {
				final Boolean isEnabled = module.getConfig().isEnabled();
				if (isEnabled == null || !isEnabled) {
					continue;
				}
			}
			if (module.load()) {
				modulesNames.put(module.getName(), module);
				modules.put(module, IModule.ON_STATE);

				if (module instanceof Listener) {
					ProxyServer.getInstance().getPluginManager().registerListener(BungeeAdminToolsPlugin.getInstance(), (Listener) module);
				}

				if(module.getConfig() != null){
					try {
						module.getConfig().save();
					} catch (final InvalidConfigurationException e) {
						e.printStackTrace();
					}
				}
			} else {
				log.severe("The " + module.getName() + " module encountered an error during his loading.");
			}
		}
	}

	public void unloadModules() {
		for (final IModule module : getLoadedModules()) {
			module.unload();
			if(module instanceof Listener){
				ProxyServer.getInstance().getPluginManager().unregisterListener((Listener) module);
			}
			modules.put(module, IModule.OFF_STATE);
		}
		ProxyServer.getInstance().getPluginManager().unregisterCommands(BungeeAdminToolsPlugin.getInstance());
		modules.clear();
	}

	public Set<IModule> getLoadedModules() {
		final Set<IModule> modulesList = new HashSet<>();
		for (final Entry<IModule, Integer> entry : modules.entrySet()) {
			if (entry.getValue().equals(IModule.ON_STATE)) {
				modulesList.add(entry.getKey());
			}
		}
		return modulesList;
	}

	public boolean isLoaded(final String name) {
		try {
			if (getModule(name) != null) {
				return true;
			}
		} catch (final InvalidModuleException e) {

		}
		return false;
	}

	public Core getCore() {
		try {
			final IModule module = getModule("core");
			if (module != null) {
				return (Core) module;
			}
		} catch (final InvalidModuleException e) {
			BungeeAdminToolsPlugin.getInstance().getLogger()
			.severe("The core module encountered a problem. Please report this to the developper :");
			e.printStackTrace();
		}
		return null;
	}

	public Ban getBanModule() throws InvalidModuleException {
		final IModule module = getModule("ban");
		if (module != null) {
			return (Ban) module;
		}
		return null;
	}

	public Mute getMuteModule() throws InvalidModuleException {
		final IModule module = getModule("mute");
		if (module != null) {
			return (Mute) module;
		}
		return null;
	}

	public Kick getKickModule() throws InvalidModuleException {
		final IModule module = getModule("kick");
		if (module != null) {
			return (Kick) module;
		}
		return null;
	}

	public Comment getCommentModule() throws InvalidModuleException {
		final IModule module = getModule("comment");
		if (module != null) {
			return (Comment) module;
		}
		return null;
	}
	
	public IModule getModule(final String name) throws InvalidModuleException {
		final IModule module = modulesNames.get(name);
		if (module != null && modules.get(module).equals(IModule.ON_STATE)) {
			return module;
		}
		throw new InvalidModuleException("Module not found or invalid");
	}
}