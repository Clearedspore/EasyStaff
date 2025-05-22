package me.clearedspore.menu.reportmenu.reportplayer.item;

import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.menu.reportmenu.evidence.EvidenceMenu;
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

public class CustomReasonItem extends Item {

    private final JavaPlugin plugin;
    private final OfflinePlayer target;
    private final ReportManager reportManager;

    public CustomReasonItem(JavaPlugin plugin, OfflinePlayer target, ReportManager reportManager) {
        this.plugin = plugin;
        this.target = target;
        this.reportManager = reportManager;
    }


    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendBlue("custom reason"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite("Click to use a custom reason"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
        player.closeInventory();
        EasyStaff.getInstance().getChatInputHandler().awaitChatInput(player, input -> {
            new EvidenceMenu(plugin, input, target, reportManager).open(player);
        });
    }
}
