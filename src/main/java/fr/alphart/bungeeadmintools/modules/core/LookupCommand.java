package fr.alphart.bungeeadmintools.modules.core;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.Permissions;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import fr.alphart.bungeeadmintools.modules.ModulesManager;
import fr.alphart.bungeeadmintools.modules.ban.BanEntry;
import fr.alphart.bungeeadmintools.modules.comment.CommentEntry;
import fr.alphart.bungeeadmintools.modules.kick.KickEntry;
import fr.alphart.bungeeadmintools.modules.mute.MuteEntry;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

import java.util.ArrayList;
import java.util.List;

import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColorAndAddPrefix;


public class LookupCommand extends BaseCommand {
    private final LookupFormatter lookupFormatter;
    private final ModulesManager modules;

    public LookupCommand() {
        lookupFormatter = new LookupFormatter();
        modules = BungeeAdminToolsPlugin.getInstance().getModules();
    }

    @CommandAlias("lookup")
    @CommandPermission(Permissions.LOOKUP)
    public void onLookup(final CommandSender sender, final String target, final String module,@Optional int page) {
        if(page == 0) {
            for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupPlayer(target,
                    sender.hasPermission(PermissionManager.Action.LOOKUP.getPermission() + ".displayip"))) {
                sender.sendMessage(msg);
            }
            return;
        }

        if(page < 0) {
            page = 1;
        }
        final var formatPlayer = "&eThe player &a{target}&e has never been {module}";
        final var formatIp = "&eThe IP &a{target}&e has never been {module}";
        try{
            final List<BaseComponent[]> message;
            switch (module) {
                case "ban" -> {
                    final List<BanEntry> bans = modules.getBanModule().getBanData(target);
                    if (!bans.isEmpty()) {
                        message = lookupFormatter.formatBanLookup(target, bans, page, false);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix((!Utils.validIP(target))
                                ? formatPlayer.replace("{target}",target).replace("{module}","banned")
                                : formatIp.replace("{target}",target).replace("{module}","banned")));
                    }
                }
                case "mute" -> {
                    final List<MuteEntry> mutes = modules.getMuteModule().getMuteData(target);
                    if (!mutes.isEmpty()) {
                        message = lookupFormatter.formatMuteLookup(target, mutes, page, false);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix((!Utils.validIP(target))
                                ? formatPlayer.replace("{target}",target).replace("{module}","muted")
                                : formatIp.replace("{target}",target).replace("{module}","muted")));
                    }
                }
                case "kick" -> {
                    final List<KickEntry> kicks = modules.getKickModule().getKickData(target);
                    if (!kicks.isEmpty()) {
                        message = lookupFormatter.formatKickLookup(target, kicks, page, false);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix((!Utils.validIP(target))
                                ? formatPlayer.replace("{target}",target).replace("{module}","kicked")
                                : formatIp.replace("{target}",target).replace("{module}","kicked")));
                    }
                }
                case "comment" -> {
                    final List<CommentEntry> comments = modules.getCommentModule().getComments(target);
                    if (!comments.isEmpty()) {
                        message = lookupFormatter.commentRowLookup(target, comments, page, false);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix((!Utils.validIP(target))
                                ? "&eThe player &a" + target + "&e has no comment about him."
                                : "&eThe IP &a" + target + "&e has no comment."));
                    }
                }
                default -> throw new InvalidModuleException("Module not found or invalid");
            }

            for (final BaseComponent[] msg : message) {
                sender.sendMessage(msg);
            }
        }catch(final InvalidModuleException e){
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @CommandAlias("stafflookup")
    @CommandPermission(Permissions.LOOKUP_STAFF)
    public void onStaffLookup(final CommandSender sender, final String staff, final String module,@Optional int page) {
        if(page == 0) {
            for (final BaseComponent[] msg : lookupFormatter.getSummaryStaffLookup(staff,
                    sender.hasPermission(PermissionManager.Action.LOOKUP.getPermission() + ".displayip"))) {
                sender.sendMessage(msg);
            }
            return;
        }

        if(page < 0) {
            page = 1;
        }
        final var formatStaff = "&b{staff}&e has never performed any operation concerning {module}.";
        try{
            final List<BaseComponent[]> message;
            final String replace = formatStaff.replace("{staff}", staff).replace("{module}", module);
            switch (module) {
                case "ban" -> {
                    final List<BanEntry> bans = modules.getBanModule().getManagedBan(staff);
                    if (!bans.isEmpty()) {
                        message = lookupFormatter.formatBanLookup(staff, bans, page, true);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix(replace));
                    }
                }
                case "mute" -> {
                    final List<MuteEntry> mutes = modules.getMuteModule().getManagedMute(staff);
                    if (!mutes.isEmpty()) {
                        message = lookupFormatter.formatMuteLookup(staff, mutes, page, true);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix(replace));
                    }
                }
                case "kick" -> {
                    final List<KickEntry> kicks = modules.getKickModule().getManagedKick(staff);
                    if (!kicks.isEmpty()) {
                        message = lookupFormatter.formatKickLookup(staff, kicks, page, true);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix(replace));
                    }
                }
                case "comment" -> {
                    final List<CommentEntry> comments = modules.getCommentModule().getManagedComments(staff);
                    if (!comments.isEmpty()) {
                        message = lookupFormatter.commentRowLookup(staff, comments, page, true);
                    } else {
                        message = new ArrayList<>();
                        message.add(formatWithColorAndAddPrefix(replace));
                    }
                }
                default -> throw new InvalidModuleException("Module not found or invalid");
            }

            for (final BaseComponent[] msg : message) {
                sender.sendMessage(msg);
            }
        }catch(final InvalidModuleException e){
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
