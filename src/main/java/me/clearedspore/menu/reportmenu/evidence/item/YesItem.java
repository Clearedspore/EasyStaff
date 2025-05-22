package me.clearedspore.menu.reportmenu.evidence.item;

import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.reports.ReportManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class YesItem extends Item {

    private final ReportManager reportManager;
    private final JavaPlugin plugin;
    private final String reason;
    private final OfflinePlayer target;

    public YesItem(ReportManager reportManager, JavaPlugin plugin, String reason, OfflinePlayer target) {
        this.reportManager = reportManager;
        this.plugin = plugin;
        this.reason = reason;
        this.target = target;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendGreen("Yes"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite("Click if you want to provide evidence (recommended) "));


        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        player.closeInventory();
        player.sendMessage(CC.sendBlue("Please provide a link for the evidence"));
        EasyStaff.getInstance().getChatInputHandler().awaitChatInput(player, input -> {
            reportManager.createReport(player, target, reason, input);
            player.sendMessage(CC.sendBlue("You have reported " + target.getName() + " for &f" + reason));
            player.sendMessage(CC.sendBlue("Thank you for helping us to keep the server safe!"));
        });
    }
}
