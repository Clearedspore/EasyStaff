package me.clearedspore.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.clearedspore.easyAPI.util.CC;
import me.clearedspore.easyAPI.util.StringUtil;
import me.clearedspore.manager.NoteManager;
import me.clearedspore.util.PS;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("note|notes|managenotes")
@CommandPermission(PS.notes)
public class NoteCommand extends BaseCommand {

    private final NoteManager noteManager;

    public NoteCommand(NoteManager noteManager) {
        this.noteManager = noteManager;
    }

    @Default
    private void onNotes(Player player){
        if(noteManager.getNotes(player).isEmpty()){
            player.sendMessage(CC.sendRed("You don't have any notes!"));
        } else {
            List<String> notes = noteManager.getNotes(player);
            player.sendMessage(CC.sendBlue("Your notes:"));
            for (String note : notes) {
                player.sendMessage(CC.send(note));
            }
        }
    }

    @Subcommand("add")
    @CommandCompletion("@players")
    @Syntax("<player> <note>")
    @CommandPermission(PS.notes_add)
    private void onNoteAdd(CommandSender player, String  targetName, String... noteParts){
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        String note = StringUtil.joinWithSpaces(noteParts);
        noteManager.addNote(target, player, note);
        player.sendMessage(CC.sendBlue("Successfully added a note to " + targetName));
    }

    @Subcommand("remove")
    @CommandCompletion("@players @noteNumbers")
    @Syntax("<player> <noteNumber>")
    @CommandPermission(PS.notes_remove)
    private void onNoteRemove(CommandSender player, String targetName, int noteNumber){
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if(noteManager.getNotes(target).isEmpty()){
            player.sendMessage(CC.sendRed("That player does not have any notes"));
            return;
        }

        noteManager.removeNote(target, noteNumber);
        player.sendMessage(CC.sendBlue("Successfully remove a note from " + targetName));
    }

    @Subcommand("check")
    @CommandCompletion("@players")
    @Syntax("<player>")
    @CommandPermission(PS.notes_check)
    private void onNotesCheck(CommandSender player, String targetName){
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if(noteManager.getNotes(target).isEmpty()){
            player.sendMessage(CC.sendRed("That player does not have any notes!"));
        } else {
            List<String> notes = noteManager.getNotes(target);
            player.sendMessage(CC.sendBlue(targetName + "'s notes:"));
            for (String note : notes) {
                player.sendMessage(CC.send(note));
            }
        }
    }


}
