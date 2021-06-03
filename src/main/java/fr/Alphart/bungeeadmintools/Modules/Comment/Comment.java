package fr.alphart.bungeeadmintools.modules.comment;

import java.io.File;
import java.io.Serial;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


import co.aikar.commands.BaseCommand;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.database.DataSourceHandler;
import fr.alphart.bungeeadmintools.database.SQLQueries;
import fr.alphart.bungeeadmintools.modules.IModule;
import fr.alphart.bungeeadmintools.modules.ModuleConfiguration;
import fr.alphart.bungeeadmintools.modules.core.Core;
import fr.alphart.bungeeadmintools.utils.Utils;
import lombok.Getter;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import com.google.common.collect.Lists;


public class Comment implements IModule {
    private final String name = "comment";
    private BaseCommand commentCommand;
    private final CommentConfig config;

    public Comment() {
        config = new CommentConfig();
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public BaseCommand getCommand() {
        return commentCommand;
    }

    @Override
    public String getMainCommand() {
        return "comment";
    }

    @Override
    public CommentConfig getConfig() {
        return config;
    }

    @Override
    public boolean load() {
        // Init table
        Statement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.createStatement();
            if (DataSourceHandler.isSQLite()) {
                for (final String commentsQuery : SQLQueries.Comments.SQLite.createTable) {
                    statement.executeUpdate(commentsQuery);
                }
            } else {
                statement.executeUpdate(SQLQueries.Comments.createTable);
            }
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        commentCommand = new CommentCommand();
        //commandHandler = new OldCommentCommand(this);
        //commandHandler.loadCommands();

        return true;
    }

    @Override
    public boolean unload() {

        return true;
    }

    public class CommentConfig extends ModuleConfiguration {
        public CommentConfig() {
            try {
                initThrowingExceptions(name);
            } catch (InvalidConfigurationException e) {
                /* The structure of a trigger has changed from 1.3.3 to 1.3.4
                 * so if an class cast exception is thrown it's probably caused by an old trigger.
                 * We're going to convert this old trigger to the new ones */
                if (e.getCause() instanceof ClassCastException) {
                    try {
                        final Configuration config;
                        final File configFile = new File(BungeeAdminToolsPlugin.getInstance().getDataFolder(), "comment.yml");
                        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

                        final Configuration triggerSection = config.getSection("triggers");
                        triggers.clear();
                        for (final String triggerName : triggerSection.getKeys()) {
                            final List<String> patterns = Collections.singletonList(triggerSection.getString(triggerName + ".pattern"));
                            final List<String> cmds = triggerSection.getStringList(triggerName + ".commands");
                            final int triggerNumber = triggerSection.getInt(triggerName + ".triggerNumber");
                            triggers.put(triggerName, new Trigger(triggerNumber, patterns, cmds));
                        }
                        save();
                    } catch (final Exception migrationException) {
                        BungeeAdminToolsPlugin.getInstance().getLogger().log(Level.SEVERE, "BAT met an error while migrating old triggers", migrationException);
                    }
                } else {
                    BungeeAdminToolsPlugin.getInstance().getLogger().log(Level.SEVERE, "BAT met an error while loading comments config", e);
                }
            }
        }

        @net.cubespace.Yamler.Config.Comments({"Triggers list",
                "Trigger name:",
                "  pattern: reason which must be provided to trigger this",
                "  commands: list of commands that should be executed when it triggers, you can use {player} variable",
                "  triggerNumber: the number at which this triggers"})
        @Getter
        private final Map<String, Trigger> triggers = new HashMap<>() {
            @Serial
            private static final long serialVersionUID = 1L;

            {
                put("example", new Trigger());
            }
        };

        @Getter
        @net.cubespace.Yamler.Config.Comment("Interval in seconds between two comments on the same player")
        private final int cooldown = 3;
    }

    /**
     * Get the notes relative to an entity
     *
     * @param entity | can be an ip or a player name
     * @return
     */
    public List<CommentEntry> getComments(final String entity) {
        List<CommentEntry> notes = Lists.newArrayList();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Comments.SQLite.getEntries
                    : SQLQueries.Comments.getEntries);
            if (Utils.validIP(entity)) {
                statement.setString(1, entity);
            } else {
                statement.setString(1, Core.getUUID(entity));
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final long date;
                if (DataSourceHandler.isSQLite()) {
                    date = resultSet.getLong("strftime('%s',date)") * 1000;
                } else {
                    date = resultSet.getTimestamp("date").getTime();
                }
                notes.add(new CommentEntry(resultSet.getInt("id"), entity, resultSet.getString("note"),
                        resultSet.getString("staff"), CommentEntry.Type.valueOf(resultSet.getString("type")),
                        date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return notes;
    }

    public List<CommentEntry> getManagedComments(final String staff) {
        List<CommentEntry> notes = Lists.newArrayList();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Comments.SQLite.getManagedEntries
                    : SQLQueries.Comments.getManagedEntries);
            statement.setString(1, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final long date;
                if (DataSourceHandler.isSQLite()) {
                    date = resultSet.getLong("strftime('%s',date)") * 1000;
                } else {
                    date = resultSet.getTimestamp("date").getTime();
                }
                String entity = Core.getPlayerName(resultSet.getString("entity"));
                if (entity == null) {
                    entity = "UUID:" + resultSet.getString("entity");
                }
                notes.add(new CommentEntry(resultSet.getInt("id"), entity, resultSet.getString("note"),
                        staff, CommentEntry.Type.valueOf(resultSet.getString("type")),
                        date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return notes;
    }

    public void insertComment(final String entity, final String comment, final CommentEntry.Type type, final String author) {
        PreparedStatement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Comments.insertEntry);
            statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
            statement.setString(2, comment);
            statement.setString(3, type.name());
            statement.setString(4, author);
            statement.executeUpdate();
            statement.close();

            // Handle the trigger system
            for (final Trigger trigger : config.getTriggers().values()) {
                for (final String pattern : trigger.getPattern()) {
                    if (pattern.isEmpty() || comment.contains(pattern)) {
                        statement = conn.prepareStatement((pattern.isEmpty())
                                ? SQLQueries.Comments.simpleTriggerCheck
                                : SQLQueries.Comments.patternTriggerCheck);
                        statement.setString(1, Core.getUUID(entity));
                        if (!pattern.isEmpty()) {
                            statement.setString(2, '%' + pattern + '%');
                        }

                        final ResultSet rs = statement.executeQuery();
                        try {
                            if (rs.next()) {
                                int count = rs.getInt("COUNT(*)");
                                if (trigger.getTriggerNumber() == count) {
                                    trigger.onTrigger(entity, comment);
                                    break;
                                }
                            }
                        } finally {
                            rs.close();
                            statement.close();
                        }

                    }
                }
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Clear all the comments and warning of an entity or the specified one
     *
     * @param entity
     * @param commentID | use -1 to remove all the comments
     * @return
     */
    public String clearComments(final String entity, final int commentID) {
        PreparedStatement statement = null;
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            if (commentID == -1) {
                statement = conn.prepareStatement(SQLQueries.Comments.clearEntries);
                statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
            } else {
                statement = conn.prepareStatement(SQLQueries.Comments.clearByID);
                statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
                statement.setInt(2, commentID);
            }
            // Check if it was successfully deleted, will be used if tried to delete an specific id comment
            boolean deleted = statement.executeUpdate() > 0;

            if (commentID != -1) {
                if (!deleted) {
                    throw new IllegalArgumentException(I18n.formatWithColor("noCommentIDFound", new String[]{entity}));
                }
                return I18n.formatWithColor("commentIDCleared", new String[]{String.valueOf(commentID), entity});
            }

            return I18n.formatWithColor("commentsCleared", new String[]{entity});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    boolean hasLastcommentCooledDown(final String entity) throws RuntimeException {
        try (Connection conn = BungeeAdminToolsPlugin.getConnection()) {
            PreparedStatement statement = conn.prepareStatement(SQLQueries.Comments.getMostRecentCommentDate);
            statement.setString(1, (Utils.validIP(entity)) ? entity : Core.getUUID(entity));
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return new Date(result.getTimestamp("date").getTime()).before(new Date(System.currentTimeMillis() - getConfig().getCooldown() * 1000));
            }
            return true;
        } catch (final SQLException e) {
            throw new RuntimeException(DataSourceHandler.handleException(e));
        }
    }
}
