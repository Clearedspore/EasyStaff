package me.clearedspore.hook.luckperms;

import me.clearedspore.feature.staffmode.VanishManager;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.DefaultContextKeys;
import org.bukkit.entity.Player;


public class VanishContext implements ContextCalculator<Player> {

    private final VanishManager vanishManager;
    private static final String KEY = "easystaff:vanished";

    public VanishContext(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        if (target.isOnline()) {
            if(vanishManager.isVanished(target.getPlayer()))
                consumer.accept(KEY, String.valueOf(true));
            } else {
                consumer.accept(KEY, String.valueOf(false));
            }
        }
    }