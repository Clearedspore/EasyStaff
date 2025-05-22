package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.filter.FilterManager;
import me.clearedspore.util.PS;
import org.bukkit.entity.Player;

@CommandAlias("filter|chatfilter")
@CommandPermission(PS.filter)
public class FilterCommand extends BaseCommand {

    private final FilterManager filterManager;

    public FilterCommand(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @Subcommand("check")
    @CommandPermission(PS.filter_check)
    @Syntax("<word>")
    public void onFilterCheck(Player player, String word){
        player.sendMessage(CC.send(filterManager.checkFilterSettings(word)));

    }
}
