package fr.alphart.bungeeadmintools.modules;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.alphart.bungeeadmintools.BAT;
import fr.alphart.bungeeadmintools.modules.BATCommand;
import fr.alphart.bungeeadmintools.modules.IModule;

public abstract class CommandHandler {
	private final IModule module;
	private final List<BATCommand> commands;

	protected CommandHandler(final IModule module) {
		this.module = module;
		commands = new ArrayList<>();
	}

	public List<BATCommand> getCommands() {
		return commands;
	}

	public void loadCommands() {
		// Get all commands and put them in a list
		final List<String> cmdName = new ArrayList<>();
		for (final Class<?> subClass : getClass().getDeclaredClasses()) {
			try {
				if(subClass.getAnnotation(BATCommand.Disable.class) != null){
					continue;
				}
				final BATCommand command = (BATCommand) subClass.getConstructors()[0].newInstance();
				commands.add(command);
				cmdName.add(command.getName());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e) {
				BAT.getInstance()
				.getLogger()
				.severe("An error happend during loading of " + module.getName()
						+ " commands please report this :");
				e.printStackTrace();
			}
		}

		// Add as default in the config file
		module.getConfig().setProvidedCmds(cmdName);

		// Sort the commands list and remove unused command
		final List<String> enabledCmds = module.getConfig().getEnabledCmds();
		commands.removeIf(cmd -> !enabledCmds.contains(cmd.getName()));
	}

}