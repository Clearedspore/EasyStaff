package me.clearedspore.feature.punishment.menu.history.warnmenu.item;

import me.clearedspore.EasyStaff;
import me.clearedspore.easyAPI.menu.Item;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.feature.punishment.Punishment;
import me.clearedspore.feature.punishment.PunishmentManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfirmItem extends Item {

    private final String warnId;
    private final OfflinePlayer target;
    private final PunishmentManager punishmentManager;
    private final JavaPlugin plugin;

    public ConfirmItem(String warnId, OfflinePlayer target, PunishmentManager punishmentManager, JavaPlugin plugin) {
        this.warnId = warnId;
        this.target = target;
        this.punishmentManager = punishmentManager;
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(CC.sendGreen("Click to confirm"));
        List<String> lore = new ArrayList<>();
        lore.add(CC.sendWhite(""));
            lore.add(CC.sendWhite("Click to remove " + warnId));
        for(Punishment punishment : punishmentManager.getAllWarns(target)){
            if(punishment.ID().equals(warnId)){
                lore.add(CC.sendWhite("Reason: " + punishment.reason()));
                String issuerName = (punishment.issuer() != null) ? punishment.issuer().getName() : "CONSOLE";
                lore.add(CC.sendWhite("Issuer: " + issuerName));
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public void onClickEvent(Player player, ClickType clickType) {
            player.closeInventory();
            player.sendMessage(CC.sendBlue("Please provide a reason to remove the warning"));

            EasyStaff.getInstance().getChatInputHandler().awaitChatInput(player, input -> {
                punishmentManager.removeWarning(player, target, warnId, input, false ,false);
                player.sendMessage(CC.sendBlue("You have removed " + target.getName() + "'s warning"));
            });
    }
}
