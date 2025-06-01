package me.clearedspore.manager;

import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.storage.PlayerData;
import me.clearedspore.util.P;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class NoteManager implements Listener {
    private final PlayerData playerData;
    private final JavaPlugin plugin;

    public NoteManager(PlayerData playerData, JavaPlugin plugin) {
        this.playerData = playerData;
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public List<String> getNotes(OfflinePlayer player) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        List<String> notes = new ArrayList<>();

        if (playerConfig.contains("notes")) {
            Set<String> noteKeys = playerConfig.getConfigurationSection("notes").getKeys(false);

            for (String key : noteKeys) {
                String text = playerConfig.getString("notes." + key + ".text");
                String issuer = playerConfig.getString("notes." + key + ".issuer");
                notes.add("&7(#" + key + ")&f - " + text + " - &7(" + issuer + ")");
            }
        }

        return notes;
    }

    public List<String> getNoteNumbers(OfflinePlayer player) {
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        if (playerConfig.contains("notes")) {
            return playerConfig.getConfigurationSection("notes").getKeys(false)
                    .stream().sorted().collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    public void addNote(OfflinePlayer player, CommandSender issuer, String text) {
        File playerfile = playerData.getPlayerFile(player);
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);


        Set<Integer> noteNumbers = new TreeSet<>();
        if (playerConfig.contains("notes")) {
            for (String key : playerConfig.getConfigurationSection("notes").getKeys(false)) {
                noteNumbers.add(Integer.parseInt(key));
            }
        }

        int noteNumber = 1;
        while (noteNumbers.contains(noteNumber)) {
            noteNumber++;
        }

        String notePath = "notes." + noteNumber;
        playerConfig.set(notePath + ".issuer", issuer.getName());
        playerConfig.set(notePath + ".text", text);
        playerData.savePlayerData(playerConfig, playerfile);
    }

    public void removeNote(OfflinePlayer player, int noteNumber) {
        File playerfile = playerData.getPlayerFile(player);
        FileConfiguration playerConfig = playerData.getPlayerConfig(player);
        String notePath = "notes." + noteNumber;

        if (playerConfig.contains(notePath)) {
            playerConfig.set(notePath, null);

            Set<String> noteKeys = playerConfig.getConfigurationSection("notes").getKeys(false);
            List<Integer> sortedKeys = noteKeys.stream()
                    .map(Integer::parseInt)
                    .sorted()
                    .collect(Collectors.toList());

            int newNoteNumber = 1;
            for (int oldNoteNumber : sortedKeys) {
                if (oldNoteNumber != newNoteNumber) {
                    String oldNotePath = "notes." + oldNoteNumber;
                    String newNotePath = "notes." + newNoteNumber;

                    playerConfig.set(newNotePath + ".issuer", playerConfig.getString(oldNotePath + ".issuer"));
                    playerConfig.set(newNotePath + ".text", playerConfig.getString(oldNotePath + ".text"));
                    playerConfig.set(oldNotePath, null);
                }
                newNoteNumber++;
            }

            playerData.savePlayerData(playerConfig, playerfile);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> notes = getNotes(player);

        if (!notes.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player players : Bukkit.getOnlinePlayers()) {
                        if (players.hasPermission(P.notes_notify)) {
                            players.sendMessage(CC.sendBlue(player.getName() + "'s notes:"));
                            for (String note : notes) {
                                players.sendMessage(CC.send(note));
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
        }
    }
}