package me.clearedspore.hook.luckperms;

import me.clearedspore.feature.staffmode.StaffModeManager;
import me.clearedspore.feature.staffmode.VanishManager;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;

public class StaffModeContext implements ContextCalculator<Player> {

    private final StaffModeManager staffModeManager;
    private static final String KEY = "easystaff:staffmode";

    public StaffModeContext(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        if (target.isOnline()) {
            if(staffModeManager.isInStaffMode(target))
                consumer.accept(KEY, String.valueOf(true));
        } else {
            consumer.accept(KEY, String.valueOf(false));
        }
    }
}