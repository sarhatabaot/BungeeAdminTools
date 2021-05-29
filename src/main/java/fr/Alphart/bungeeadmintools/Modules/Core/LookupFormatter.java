package fr.alphart.bungeeadmintools.modules.core;



import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.alphart.bungeeadmintools.BungeeAdminToolsPlugin;
import fr.alphart.bungeeadmintools.I18n.I18n;
import fr.alphart.bungeeadmintools.modules.InvalidModuleException;
import fr.alphart.bungeeadmintools.modules.ModulesManager;
import fr.alphart.bungeeadmintools.modules.ban.BanEntry;
import fr.alphart.bungeeadmintools.modules.comment.CommentEntry;
import fr.alphart.bungeeadmintools.modules.kick.KickEntry;
import fr.alphart.bungeeadmintools.modules.mute.MuteEntry;
import fr.alphart.bungeeadmintools.utils.FormatUtils;
import fr.alphart.bungeeadmintools.utils.MojangAPIProvider;
import fr.alphart.bungeeadmintools.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;

import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColor;
import static fr.alphart.bungeeadmintools.I18n.I18n.formatWithColorAndAddPrefix;


public class LookupFormatter {
    private ModulesManager modules;
    private static final int entriesPerPage = 15;
    private final String lookupHeader;
    private final String lookupFooter;
    
    public LookupFormatter(){
        lookupHeader = formatWithColor("perModuleLookupHeader");
        lookupFooter = formatWithColor("perModuleLookupFooter");
        modules = BungeeAdminToolsPlugin.getInstance().getModules();
    }

    public List<BaseComponent[]> getSummaryLookupPlayer(final String pName, final boolean displayIP) {
        // Gather players data related to each modules
        final EntityEntry pDetails = new EntityEntry(pName);

        if (!pDetails.exist()) {
            final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
            returnedMsg.add(formatWithColorAndAddPrefix("playerNotFound"));
            return returnedMsg;
        }

        final EntityEntry ipDetails = new EntityEntry(Core.getPlayerIP(pName));
        boolean isBan = false;
        boolean isBanIP = false;
        int bansNumber = 0;
        final List<String> banServers = Lists.newArrayList();
        final List<String> banIPServers = Lists.newArrayList();
        boolean isMute = false;
        boolean isMuteIP = false;
        int mutesNumber = 0;
        final List<String> muteServers = Lists.newArrayList();
        final List<String> muteIPServers = Lists.newArrayList();
        int kicksNumber = 0;
        // Compute player's state (as well as his ip) concerning ban and mute
        for (final BanEntry banEntry : pDetails.getBans()) {
            if (banEntry.active()) {
                isBan = true;
                banServers.add(banEntry.server());
            }
        }
        for (final BanEntry banEntry : ipDetails.getBans()) {
            if (banEntry.active()) {
                isBanIP = true;
                banIPServers.add(banEntry.server());
            }
        }
        for (final MuteEntry muteEntry : pDetails.getMutes()) {
            if (muteEntry.active()) {
                isMute = true;
                muteServers.add(muteEntry.server());
            }
        }
        for (final MuteEntry muteEntry : ipDetails.getMutes()) {
            if (muteEntry.active()) {
                isMuteIP = true;
                muteIPServers.add(muteEntry.server());
            }
        }
        bansNumber = pDetails.getBans().size() + ipDetails.getBans().size();
        mutesNumber = pDetails.getMutes().size() + ipDetails.getMutes().size();
        kicksNumber = pDetails.getKicks().size();
        
        // Load the lookup pattern
        final String lookupPattern = formatWithColor("playerLookup");
        
        // Initialize all the strings to prepare the big replace
        String connection_state;
        if (BungeeAdminToolsPlugin.getInstance().getRedis().isRedisEnabled()) {
                UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
                if(pUUID != null && RedisBungee.getApi().isPlayerOnline(pUUID)){
                    ServerInfo si = RedisBungee.getApi().getServerFor(pUUID);
                    connection_state = formatWithColor("connectionStateOnline").replace("{server}", si != null ? si.getName() : "unknown state");
                }else{
                    connection_state = formatWithColor("connectionStateOffline");
                }
        } else {
            if(ProxyServer.getInstance().getPlayer(pName) != null){
                connection_state = formatWithColor("connectionStateOnline")
                        .replace("{server}", ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName());
            }else{
                connection_state = formatWithColor("connectionStateOffline");
            }

        }
        
        final String joinChar = "&f, &3";
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : formatWithColor("none");
        final String banip_servers = !banIPServers.isEmpty()
                ? Joiner.on(joinChar).join(banIPServers).toLowerCase()
                : formatWithColor("none");
        final String mute_servers = !muteServers.isEmpty()
                ? Joiner.on(joinChar).join(muteServers).toLowerCase()
                : formatWithColor("none");
        final String muteip_servers = !muteIPServers.isEmpty()
                ? Joiner.on(joinChar).join(muteIPServers).toLowerCase()
                : formatWithColor("none");

        final String first_login = pDetails.getFirstLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getFirstLogin().getTime()))
                : formatWithColor("unknownDate");
        final String last_login = pDetails.getLastLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getLastLogin().getTime()))
                : formatWithColor("unknownDate");
        final String last_ip = !"0.0.0.0".equals(pDetails.getLastIP())
                ? ((displayIP) ? pDetails.getLastIP() : formatWithColor("hiddenIp"))
                : formatWithColor("unknownIp");
                
        String name_history_list;
        // Create a function for that or something better than a big chunk of code inside the lookup
        if(ProxyServer.getInstance().getConfig().isOnlineMode()){
            try{
                name_history_list = Joiner.on("&e, &a").join(MojangAPIProvider.getPlayerNameHistory(pName));
            }catch(final RuntimeException e){
                name_history_list = "unable to fetch player's name history. Check the logs";
                BungeeAdminToolsPlugin.getInstance().getLogger().severe("An error occured while fetching " + pName + "'s name history from mojang servers."
                        + "Please report this : ");
                e.printStackTrace();
            }
        }else{
            name_history_list = "offline server";
        }
        
        int commentsNumber = pDetails.getComments().size();
        String last_comments = "";
        // We need to parse the number of last comments from the lookup pattern
        final Pattern lastCommentsPattern = Pattern.compile("(?:.|\n)*?\\{last_comments:(\\d*)\\}(?:.|\n)*?");
        final Matcher matcher = lastCommentsPattern.matcher(lookupPattern);
        try{
            if(!matcher.matches()){
                throw new NumberFormatException();
            }
            int nLastComments = Integer.parseInt(matcher.group(1));
            if(nLastComments < 1){
                throw new NumberFormatException();
            }
            int i = 0;
            for(final CommentEntry comm : pDetails.getComments()){
                last_comments += I18n.formatWithColor("commentRow", new String[]{String.valueOf(comm.getID()),
                        (comm.getType() == CommentEntry.Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
                        comm.getFormattedDate(), comm.getAuthor()});
                i++;
                if(i == 3){
                    break;
                }
            }
            if(last_comments.isEmpty()){
                last_comments = formatWithColor("none");
            }
        }catch(final NumberFormatException e){
            last_comments = "Unable to parse the number of last_comments";
        }

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                lookupPattern
                .replace("{connection_state}", connection_state)
                .replace("{ban_servers}", ban_servers).replace("{banip_servers}", banip_servers)
                .replace("{mute_servers}", mute_servers).replace("{muteip_servers}", muteip_servers)
                .replace("{first_login}", first_login).replace("{last_login}", last_login).replace("{last_ip}", last_ip)
                .replace("{bans_number}", String.valueOf(bansNumber)).replace("{mutes_number}", String.valueOf(mutesNumber))
                .replace("{kicks_number}", String.valueOf(kicksNumber)).replace("{comments_number}", String.valueOf(commentsNumber))
                .replace("{name_history_list}", name_history_list).replaceAll("\\{last_comments:\\d\\}", last_comments)
                .replace("{player}", pName).replace("{uuid}", Core.getUUID(pName))
                // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                .replace("¤", " ").replace("\\¤", "¤")
                ));
    }

    public List<BaseComponent[]> getSummaryLookupIP(final String ip) {
        final EntityEntry ipDetails = new EntityEntry(ip);
        if (!ipDetails.exist()) {
            final List<BaseComponent[]> returnedMsg = new ArrayList<BaseComponent[]>();
            returnedMsg.add(formatWithColorAndAddPrefix("unknownIp"));
            return returnedMsg;
        }
        boolean isBan = false;
        int bansNumber = 0;
        final List<String> banServers = new ArrayList<String>();
        boolean isMute = false;
        int mutesNumber = 0;
        final List<String> muteServers = new ArrayList<String>();
        if (!ipDetails.getBans().isEmpty()) {
            for (final BanEntry banEntry : ipDetails.getBans()) {
                if (banEntry.active()) {
                    isBan = true;
                    banServers.add(banEntry.server());
                }
            }
            bansNumber = ipDetails.getBans().size();
        }
        if (!ipDetails.getMutes().isEmpty()) {
            for (final MuteEntry muteEntry : ipDetails.getMutes()) {
                if (muteEntry.active()) {
                    isMute = true;
                    muteServers.add(muteEntry.server());
                }
            }
            mutesNumber = ipDetails.getMutes().size();
        }

        // Initialize all strings
        final String joinChar = "&f, &3";
        final String ip_users = !ipDetails.getUsers().isEmpty()
                ? Joiner.on(joinChar).join(ipDetails.getUsers())
                : formatWithColor("none");
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : formatWithColor("none");
        final String mute_servers = !muteServers.isEmpty()
                ? Joiner.on(joinChar).join(muteServers).toLowerCase()
                : formatWithColor("none");
        
        String replacedString = formatWithColor("ipLookup")
                .replace("{ban_servers}", ban_servers).replace("{mute_servers}", mute_servers)
                .replace("{bans_number}", String.valueOf(bansNumber)).replace("{mutes_number}", String.valueOf(mutesNumber))
                .replace("{ip}", ip).replace("{ip_users}", ip_users)
                // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                .replace("¤", " ").replace("\\¤", "¤");
                
        if(replacedString.contains("{ip_location}")){
            String ipLocation = "";
            try{
                ipLocation = Utils.getIpDetails(ip);
            }catch(final Exception e){
                BungeeAdminToolsPlugin.getInstance().getLogger().log(Level.SEVERE,
                        "Error while fetching ip location from the API. Please report this :", e);
                ipLocation = "unresolvable ip location. Check your logs";
            }
            replacedString = replacedString.replace("{ip_location}", ipLocation);
        }

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                replacedString));
    }
    
    public List<BaseComponent[]> getSummaryStaffLookup(final String staff, final boolean displayID) {
        int bans_number = 0;
        int unbans_number = 0;
        int mutes_number = 0;
        int unmutes_number = 0;
        int kicks_number = 0;
        int comments_number = 0;
        int warnings_number = 0;
        try{
            if(modules.isLoaded("ban")){
                for(final BanEntry ban : modules.getBanModule().getManagedBan(staff)){
                    if(staff.equalsIgnoreCase(ban.staff())){
                        bans_number++;
                    }
                    if(staff.equalsIgnoreCase(ban.unbanStaff())){
                        unbans_number++;
                    }
                }
            }
            if(modules.isLoaded("mute")){
                for(final MuteEntry mute : modules.getMuteModule().getManagedMute(staff)){
                    if(staff.equalsIgnoreCase(mute.staff())){
                        mutes_number++;
                    }
                    if(staff.equalsIgnoreCase(mute.unmuteStaff())){
                        unmutes_number++;
                    }
                }
            }
            if(modules.isLoaded("kick")){
                for(final KickEntry kick : modules.getKickModule().getManagedKick(staff)){
                    if(staff.equalsIgnoreCase(kick.staff())){
                        kicks_number++;
                    }
                }
            }
            if(modules.isLoaded("comment")){
                for(final CommentEntry mute : modules.getCommentModule().getManagedComments(staff)){
                    if(mute.getType() == CommentEntry.Type.NOTE){
                        comments_number++;
                    }
                    else{
                        warnings_number++;
                    }
                }
            }
        }catch(final InvalidModuleException e){
            e.printStackTrace();
        }

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                formatWithColor("staffLookup")
                .replace("{bans_number}", String.valueOf(bans_number)).replace("{unbans_number}", String.valueOf(unbans_number))
                .replace("{mutes_number}", String.valueOf(mutes_number)).replace("{unmutes_number}", String.valueOf(unmutes_number))
                .replace("{kicks_number}", String.valueOf(kicks_number))
                .replace("{comments_number}", String.valueOf(comments_number))
                .replace("{warnings_number}", String.valueOf(warnings_number))
                .replace("{staff}", staff).replace("{uuid}", Core.getUUID(staff))
                .replace("¤", " ").replace("\\¤", "¤")
                ));
    }
    
    public List<BaseComponent[]> formatBanLookup(final String entity, final List<BanEntry> bans, 
            int page, final boolean staffLookup) throws InvalidModuleException {
        final StringBuilder msg = new StringBuilder();

        int totalPages = (int) Math.ceil((double)bans.size()/entriesPerPage);
        if(bans.size() > entriesPerPage){
            if(page > totalPages){
                page = totalPages;
            }
            int beginIndex = (page - 1) * entriesPerPage;
            int endIndex = Math.min(beginIndex + entriesPerPage, bans.size());
            for(int i=bans.size() -1; i > 0; i--){
                if(i >= beginIndex && i < endIndex){
                    continue;
                }
                bans.remove(i);
            }
        }
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Ban")
                .replace("{page}", page + "/" + totalPages));
        
        boolean isBan = false;
        for (final BanEntry banEntry : bans) {
            if (banEntry.active()) {
                isBan = true;
            }
        }

        // We begin with active ban
        if(isBan){
            msg.append("&6&lActive bans: &e");
            final Iterator<BanEntry> it = bans.iterator();
            while(it.hasNext()){
                final BanEntry ban = it.next();
                if(!ban.active()){
                    break;
                }
                final String begin = Core.defaultDF.format(ban.beginDate());
                final String server = ban.server();
                final String reason = ban.reason();
                final String end;
                if(ban.endDate() == null){
                    end = "permanent ban";
                }else{
                    end = Core.defaultDF.format(ban.endDate());
                }
                
                msg.append("\n");
                if(staffLookup){
                    msg.append(I18n.formatWithColor("activeStaffBanLookupRow",
                            new String[] { ban.entity(), begin, server, reason, end}));
                }else{
                    msg.append(I18n.formatWithColor("activeBanLookupRow",
                            new String[] { begin, server, reason, ban.staff(), end}));
                }
                it.remove();
            }
        }
        
        if(!bans.isEmpty()){
            msg.append("\n&7&lArchive bans: &e");
            for(final BanEntry ban : bans){
                final String begin = Core.defaultDF.format(ban.beginDate());
                final String server = ban.server();
                final String reason = ban.reason();
                
                final String endDate;
                if(ban.endDate() == null){
                    endDate = Core.defaultDF.format(ban.unbanDate());
                }else{
                    endDate = Core.defaultDF.format(ban.endDate());
                }
                final String unbanReason = ban.unbanReason();
                String unbanStaff = ban.unbanStaff();
                if(unbanStaff == null){
                    unbanStaff = "temporary ban";
                }
                
                msg.append("\n");
                if(staffLookup){
                    msg.append(I18n.formatWithColor("archiveStaffBanLookupRow",
                            new String[] { ban.entity(), begin, server, reason, endDate, unbanReason, unbanStaff}));
                }else{
                    msg.append(I18n.formatWithColor((staffLookup) ? "archiveStaffBanLookupRow" : "archiveBanLookupRow",
                            new String[] { begin, server, reason, ban.staff(), endDate, unbanReason, unbanStaff}));
                }
                
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Ban")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> formatMuteLookup(final String entity, final List<MuteEntry> mutes,
            int page, final boolean staffLookup) throws InvalidModuleException {
        final StringBuilder msg = new StringBuilder();

        int totalPages = (int) Math.ceil((double)mutes.size()/entriesPerPage);
        if(mutes.size() > entriesPerPage){
            if(page > totalPages){
                page = totalPages;
            }
            int beginIndex = (page - 1) * entriesPerPage;
            int endIndex = Math.min(beginIndex + entriesPerPage, mutes.size());
            for(int i=mutes.size() -1; i > 0; i--){
                if(i >= beginIndex && i < endIndex){
                    continue;
                }
                mutes.remove(i);
            }
        }
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Mute")
                .replace("{page}", page + "/" + totalPages));
        
        boolean isMute = false;
        for (final MuteEntry muteEntry : mutes) {
            if (muteEntry.active()) {
                isMute = true;
                break;
            }
        }

        // We begin with active ban
        if(isMute){
            msg.append("&6&lActive mutes: &e");
            final Iterator<MuteEntry> it = mutes.iterator();
            while(it.hasNext()){
                final MuteEntry mute = it.next();
                if(!mute.active()){
                    break;
                }
                final String begin = Core.defaultDF.format(mute.beginDate());
                final String server = mute.server();
                final String reason = mute.reason();
                final String end;
                if(mute.endDate() == null){
                    end = "permanent mute";
                }else{
                    end = Core.defaultDF.format(mute.endDate());
                }
                
                msg.append("\n");
                if(staffLookup){
                    msg.append(I18n.formatWithColor("activeStaffMuteLookupRow",
                            new String[] { mute.entity(), begin, server, reason, end}));
                }else{
                    msg.append(I18n.formatWithColor("activeMuteLookupRow",
                            new String[] { begin, server, reason, mute.staff(), end}));
                }
                it.remove();
            }
        }
        
        if(!mutes.isEmpty()){
            msg.append("\n&7&lArchive mutes: &e");
            for(final MuteEntry mute : mutes){
                final String begin = Core.defaultDF.format(mute.beginDate());
                final String server = mute.server();
                final String reason = mute.reason();
                
                final String unmuteDate;
                if(mute.unmuteDate() == null){
                    unmuteDate = Core.defaultDF.format(mute.endDate());
                }else{
                    unmuteDate = Core.defaultDF.format(mute.unmuteDate());
                }
                final String unmuteReason = mute.unmuteReason();
                String unmuteStaff = mute.unmuteStaff();
                if(unmuteStaff.equals("null")){
                    unmuteStaff = "temporary mute";
                }
                
                msg.append("\n");
                if(staffLookup){
                    msg.append(I18n.formatWithColor("archiveStaffMuteLookupRow",
                            new String[] { mute.entity(), begin, server, reason, unmuteDate, unmuteReason, unmuteStaff}));
                }else{
                    msg.append(I18n.formatWithColor("archiveMuteLookupRow",
                            new String[] { begin, server, reason, mute.staff(), unmuteDate, unmuteReason, unmuteStaff}));
                }
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Mute")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> formatKickLookup(final String entity, final List<KickEntry> kicks,
            int page, final boolean staffLookup) throws InvalidModuleException {
        final StringBuilder msg = new StringBuilder();

        int totalPages = (int) Math.ceil((double)kicks.size()/entriesPerPage);
        if(kicks.size() > entriesPerPage){
            if(page > totalPages){
                page = totalPages;
            }
            int beginIndex = (page - 1) * entriesPerPage;
            int endIndex = Math.min(beginIndex + entriesPerPage, kicks.size());
            for(int i=kicks.size() -1; i > 0; i--){
                if(i >= beginIndex && i < endIndex){
                    continue;
                }
                kicks.remove(i);
            }
        }
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Kick")
                .replace("{page}", page + "/" + totalPages));
        
        msg.append("&6&lKick list :");
        
        for(final KickEntry kick : kicks){
            final String date = Core.defaultDF.format(kick.date());
            final String server = kick.server();
            final String reason = kick.reason();
            
            msg.append("\n");
            if(staffLookup){
                msg.append(I18n.formatWithColor("kickStaffLookupRow",
                        new String[] { kick.entity(), date, server, reason}));
            }else{
                msg.append(I18n.formatWithColor("kickLookupRow",
                        new String[] { date, server, reason, kick.staff()}));
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Kick")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
    public List<BaseComponent[]> commentRowLookup(final String entity, final List<CommentEntry> comments,
            int page, final boolean staffLookup) throws InvalidModuleException {{
        final StringBuilder msg = new StringBuilder();

        int totalPages = (int) Math.ceil((double)comments.size()/entriesPerPage);
        if(comments.size() > entriesPerPage){
            if(page > totalPages){
                page = totalPages;
            }
            int beginIndex = (page - 1) * entriesPerPage;
            int endIndex = Math.min(beginIndex + entriesPerPage, comments.size());
            for(int i=comments.size() -1; i > 0; i--){
                if(i >= beginIndex && i < endIndex){
                    continue;
                }
                comments.remove(i);
            }
        }
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Comment")
                .replace("{page}", page + "/" + totalPages));
        
        msg.append("&6&lComment list :");
        
        for(final CommentEntry comm : comments){
            msg.append("\n");
            if(staffLookup){
                msg.append(I18n.formatWithColor("commentStaffRow", new String[]{String.valueOf(comm.getID()),
                        (comm.getType() == CommentEntry.Type.NOTE) ? "&eComment" : "&cWarning",
                        comm.getEntity(), comm.getContent(), comm.getFormattedDate()}));
            }
            else{
                msg.append(I18n.formatWithColor("commentRow", new String[]{String.valueOf(comm.getID()),
                    (comm.getType() == CommentEntry.Type.NOTE) ? "&eComment" : "&cWarning", comm.getContent(),
                    comm.getFormattedDate(), comm.getAuthor()}));
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Comment")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtils.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
    
}
}
