package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommandPermission(PS.cps)
@CommandAlias("checkcps|cps")
public class CPScheckCommand extends BaseCommand implements Listener {

    private final Map<UUID, Integer> clickCounts = new ConcurrentHashMap<>();
    private final Set<UUID> playersBeingChecked = new ConcurrentHashMap<UUID, Boolean>().keySet(Boolean.TRUE);
    private final Map<UUID, Integer> highestCPS = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> checkerToTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    public CPScheckCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onCpsCheck(Player checker, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            checker.sendMessage(CC.sendRed("Player not found."));
            return;
        }

        if (playersBeingChecked.contains(target.getUniqueId())) {
            checker.sendMessage(CC.sendRed("This player is already being checked."));
            return;
        }

        if (checkerToTarget.containsKey(checker.getUniqueId())) {
            checker.sendMessage(CC.sendRed("You are already checking someone's CPS."));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        clickCounts.put(targetUUID, 0);
        highestCPS.put(targetUUID, 0);
        playersBeingChecked.add(targetUUID);
        checkerToTarget.put(checker.getUniqueId(), targetUUID);
        lastCheckTime.put(targetUUID, System.currentTimeMillis());

        Bukkit.getLogger().info("[CPSCheck] Starting CPS check for " + target.getName() + " by " + checker.getName());
        Bukkit.getLogger().info("[CPSCheck] Target UUID added to playersBeingChecked: " + playersBeingChecked.contains(targetUUID));

        checker.sendMessage(CC.sendBlue("CPS check started for " + target.getName() + "."));
        target.sendMessage(CC.sendBlue("A staff member is checking your CPS. Please click normally."));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(PS.notify) && !staff.equals(checker)) {
                staff.sendMessage(CC.send("&7[Staff] &f" + checker.getName() + " is checking " + target.getName() + "'s CPS"));
            }
        }

        new BukkitRunnable() {
            int secondsPassed = 0;
            int[] intervalCPS = new int[4];
            int[] intervalClicks = new int[4];

            @Override
            public void run() {
                Bukkit.getLogger().info("[CPSCheck] Timer tick for " + target.getName() + ", secondsPassed=" + secondsPassed);
                
                if (!target.isOnline() || !playersBeingChecked.contains(targetUUID)) {
                    Bukkit.getLogger().info("[CPSCheck] Cancelling check: player online=" + target.isOnline() + 
                                           ", in checked set=" + playersBeingChecked.contains(targetUUID));
                    cancel();
                    return;
                }

                if (secondsPassed >= 20) {
                    int totalClicks = 0;
                    for (int clicks : intervalClicks) {
                        totalClicks += clicks;
                    }
                    
                    double averageCPS = totalClicks / 20.0;
                    int highest = highestCPS.getOrDefault(targetUUID, 0);

                    Bukkit.getLogger().info("[CPSCheck] Check complete for " + target.getName() + 
                                           ", totalClicks=" + totalClicks + 
                                           ", averageCPS=" + averageCPS + 
                                           ", highest=" + highest);

                    checker.sendMessage(CC.sendBlue("CPS check complete for " + target.getName() + "!"));
                    checker.sendMessage(CC.sendBlue("Average CPS: &a" + String.format("%.1f", averageCPS)));
                    checker.sendMessage(CC.sendBlue("Highest CPS: &a" + highest));

                    StringBuilder breakdown = new StringBuilder(CC.sendBlue("Interval breakdown: "));
                    for (int i = 0; i < 4; i++) {
                        breakdown.append(getCPSColor(intervalCPS[i])).append(intervalCPS[i]).append(" CPS");
                        if (i < 3) breakdown.append(ChatColor.RESET).append(", ");
                    }
                    checker.sendMessage(breakdown.toString());

                    target.sendMessage(CC.sendBlue("CPS check complete. Thank you for your cooperation."));

                    clickCounts.remove(targetUUID);
                    highestCPS.remove(targetUUID);
                    playersBeingChecked.remove(targetUUID);
                    checkerToTarget.remove(checker.getUniqueId());
                    lastCheckTime.remove(targetUUID);
                    cancel();
                    return;
                }

                int currentInterval = secondsPassed / 5;

                int currentClicks = clickCounts.getOrDefault(targetUUID, 0);

                Bukkit.getLogger().info("[CPSCheck] Current interval " + currentInterval + 
                                       " for " + target.getName() + 
                                       ", clicks=" + currentClicks);

                intervalClicks[currentInterval] = currentClicks;

                int currentCPS = currentClicks / 5;

                Bukkit.getLogger().info("[CPSCheck] Calculated CPS: " + currentCPS);

                intervalCPS[currentInterval] = currentCPS;
                highestCPS.put(targetUUID, Math.max(highestCPS.getOrDefault(targetUUID, 0), currentCPS));
                clickCounts.put(targetUUID, 0);
                lastCheckTime.put(targetUUID, System.currentTimeMillis());

                StringBuilder progress = new StringBuilder(CC.send("&aCPS checker: &f["));
                for (int i = 0; i < 4; i++) {
                    if (i < currentInterval) {
                        String color = getCPSColor(intervalCPS[i]);
                        progress.append(color).append(intervalCPS[i]).append("cps");
                    } else if (i == currentInterval) {
                        progress.append(ChatColor.GREEN).append("●●●");
                    } else {
                        progress.append(ChatColor.GRAY).append("---");
                    }

                    if (i < 3) {
                        progress.append(ChatColor.RESET).append("|");
                    }
                }
                progress.append(ChatColor.RESET).append("]");

                checker.sendMessage(progress.toString());

                checker.sendMessage(CC.send("&7Current clicks this interval: &f" + currentClicks));

                secondsPassed += 5;
            }

            private String getCPSColor(int cps) {
                if (cps >= 16) return ChatColor.RED.toString();
                if (cps >= 12) return ChatColor.GOLD.toString();
                if (cps >= 8) return ChatColor.YELLOW.toString();
                return ChatColor.GREEN.toString();
            }
        }.runTaskTimer(plugin, 0, 100);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (playersBeingChecked.contains(playerUUID)) {
            Bukkit.getLogger().info("[CPSCheck] Interact event for " + player.getName() + ": " + event.getAction());

            if (event.getAction() == Action.LEFT_CLICK_AIR || 
                event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR || 
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                incrementClickCount(playerUUID);

                UUID checkerUUID = null;
                for (Map.Entry<UUID, UUID> entry : checkerToTarget.entrySet()) {
                    if (entry.getValue().equals(playerUUID)) {
                        checkerUUID = entry.getKey();
                        break;
                    }
                }
                
                if (checkerUUID != null) {
                    Player checker = Bukkit.getPlayer(checkerUUID);
                    if (checker != null && checker.isOnline()) {
                        checker.sendMessage(CC.send("&8[Debug] &7Click detected: &f" + event.getAction()));
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (playersBeingChecked.contains(playerUUID)) {
            Bukkit.getLogger().info("[CPSCheck] Animation event for " + player.getName() + ": " + event.getAnimationType());

            if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
                incrementClickCount(playerUUID);

                UUID checkerUUID = null;
                for (Map.Entry<UUID, UUID> entry : checkerToTarget.entrySet()) {
                    if (entry.getValue().equals(playerUUID)) {
                        checkerUUID = entry.getKey();
                        break;
                    }
                }
                
                if (checkerUUID != null) {
                    Player checker = Bukkit.getPlayer(checkerUUID);
                    if (checker != null && checker.isOnline()) {
                        checker.sendMessage(CC.send("&8[Debug] &7Animation detected: &fARM_SWING"));
                    }
                }
            }
        }
    }
    
    private void incrementClickCount(UUID playerUUID) {
        int currentCount = clickCounts.getOrDefault(playerUUID, 0);
        int newCount = currentCount + 1;

        clickCounts.put(playerUUID, newCount);

        Bukkit.getLogger().info("[CPSCheck] Click count for " + playerUUID + " increased from " + currentCount + " to " + newCount);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            Bukkit.getLogger().info("[CPSCheck] That's " + player.getName() + "'s click count");
        }

        int verifyCount = clickCounts.getOrDefault(playerUUID, -1);
        if (verifyCount != newCount) {
            Bukkit.getLogger().warning("[CPSCheck] Click count verification failed! Expected " + newCount + " but got " + verifyCount);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playersBeingChecked.contains(playerUUID)) {
            UUID checkerUUID = null;
            for (Map.Entry<UUID, UUID> entry : checkerToTarget.entrySet()) {
                if (entry.getValue().equals(playerUUID)) {
                    checkerUUID = entry.getKey();
                    break;
                }
            }

            clickCounts.remove(playerUUID);
            highestCPS.remove(playerUUID);
            playersBeingChecked.remove(playerUUID);
            lastCheckTime.remove(playerUUID);
            
            if (checkerUUID != null) {
                checkerToTarget.remove(checkerUUID);
                Player checker = Bukkit.getPlayer(checkerUUID);
                if (checker != null && checker.isOnline()) {
                    checker.sendMessage(CC.sendRed("CPS check cancelled: " + player.getName() + " has logged out."));
                }
            }

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(PS.notify)) {
                    staff.sendMessage(CC.sendBlue("[Staff] &f" + player.getName() + " &#00CCDEhas logged out while a staff member was checking their CPS"));
                }
            }
        }

        if (checkerToTarget.containsKey(playerUUID)) {
            UUID targetUUID = checkerToTarget.get(playerUUID);
            Player target = Bukkit.getPlayer(targetUUID);

            clickCounts.remove(targetUUID);
            highestCPS.remove(targetUUID);
            playersBeingChecked.remove(targetUUID);
            lastCheckTime.remove(targetUUID);
            checkerToTarget.remove(playerUUID);
        }
    }
}