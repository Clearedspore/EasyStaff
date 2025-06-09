package me.clearedspore.util;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class P {

    public static final String easystaff = "easystaff";
    public static final String reload = "easystaff.reload";
    public static final String exempt = "easystaff.exempt";
    public static final String exempt_add = "easystaff.exempt.add";
    public static final String exempt_remove = "easystaff.exempt.remove";
    public static final String clear_history = "easystaff.punishments.history.clear";
    public static final String update_notify = "easystaff.update.notify";
    public static final String version = "easystaff.version";

    public static final String playerdata_generate = "easystaff.playerdata.generate";
    public static final String playerdata_reset = "easystaff.playerdata.reset";


    public static final String freeze = "easystaff.freeze";
    public static final String freeze_notify = "easystaff.freeze.notify";

    public static final String ban = "easystaff.punishments.ban";
    public static final String unban = "easystaff.punishments.unban";
    public static final String tempban = "easystaff.punishments.tempban";
    public static final String kick = "easystaff.punishments.kick";
    public static final String punish = "easystaff.punishments.punish";
    public static final String punish_ban = "easystaff.punishments.punish.ban";
    public static final String punish_kick = "easystaff.punishments.punish.kick";
    public static final String punish_mute = "easystaff.punishments.punish.mute";
    public static final String punish_warn = "easystaff.punishments.punish.warn";
    public static final String punish_tempmute = "easystaff.punishments.punish.tempmute";
    public static final String punish_tempban = "easystaff.punishments.punish.tempban";
    public static final String warn = "easystaff.punishments.warn";
    public static final String unwarn = "easystaff.punishments.unwarn";
    public static final String mute = "easystaff.punishments.mute";
    public static final String unmute = "easystaff.punishments.unmute";
    public static final String tempmute = "easystaff.punishments.tempmute";
    public static final String silent = "easystaff.punishments.silent";
    public static final String high_silent = "easystaff.punishments.highsilent";
    public static final String punish_staff = "easystaff.punishments.staff";
    public static final String removeown_bypass = "easystaff.punishments.bypass.removeown";
    public static final String punish_notify = "easystaff.punishments.notify";
    public static final String punish_notify_high = "easystaff.punishments.notify.high";
    public static final String history = "easystaff.punishments.history";
    public static final String history_others = "easystaff.punishments.history.others";
    public static final String punishments_hide = "easystaff.punishments.hide";

    public static final String report_player = "easystaff.reports.reportplayer";
    public static final String reports = "easystaff.reports.manage";
    public static final String reports_notify = "easystaff.reports.notify";

    public static final String alts = "easystaff.alts";
    public static final String requesthelp = "easystaff.requesthelp";
    public static final String staff_view = "easystaff.staff.helpview";
    public static final String staff_settings = "easystaff.setting";
    public static final String notify = "easystaff.notify";

    public static final String filter = "easystaff.filter";
    public static final String filter_notify = "easystaff.filter.notify";
    public static final String filter_check = "easystaff.filter.check";

    public static final String maintenance = "easystaff.maintenance";
    public static final String maintenance_add = "easystaff.maintenance.add";
    public static final String maintenance_remove = "easystaff.maintenance.remove";
    public static final String maintenance_kickall = "easystaff.maintenance.kickall";
    public static final String maintenance_logs = "easystaff.maintenance.logs";
    public static final String maintenance_info = "easystaff.maintenance.info";
    public static final String maintenance_toggle = "easystaff.maintenance.toggle";

    public static final String notes = "easystaff.notes";
    public static final String notes_add = "easystaff.notes.add";
    public static final String notes_remove = "easystaff.notes.remove";
    public static final String notes_check = "easystaff.notes.check";
    public static final String notes_notify = "easystaff.notes.notify";

    public static final String teleport = "easystaff.teleport";
    public static final String teleport_cords = "easystaff.teleport.coordinates";
    public static final String teleport_others = "easystaff.teleport.others";


    public static final String vanish = "easystaff.vanish.toggle";
    public static final String vanish_others = "easystaff.vanish.others";
    public static final String vanish_see = "easystaff.vanish.see";

    public static final String channel_reload = "easystaff.channel.reload";
    public static final String channel_list = "easystaff.channel.list";
    public static final String channel = "easystaff.channel";

    public static final String invsee = "easystaff.invsee";
    public static final String invsee_admin = "easystaff.invsee.admin";

    public static final String staffmode = "easystaff.staffmode";

    public static final String whois = "easystaff.whois";

    public static final String block_name = "easystaff.blockname";
    public static final String remove_blocked_name = "easystaff.blockname.remove";
    public static final String blocked_name_list = "easystaff.blockname.list";

    public static final String alerts = "easystaff.alerts";
    public static final String admin = "easystaff.admin";

    public static final String discord_link = "easystaff.discord.link";

    public static final String chat_clear = "easystaff.chat.clear";
    public static final String chat_clear_bypass = "easystaff.chat.clearbypass";
    public static final String chat_toggle = "easystaff.chat.toggle";
    public static final String chat_toggle_bpyass = "easystaff.chat.togglebypass";
    public static final String chat = "easystaff.chat";

    public static final String cps = "easystaff.cps";

    public static void registerPermissions() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        Field[] fields = P.class.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && field.getType().equals(String.class)) {
                try {
                    String perm = (String) field.get(null);
                    Permission permission = new Permission(perm);
                    if (pluginManager.getPermission(permission.getName()) == null) {
                        pluginManager.addPermission(permission);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}