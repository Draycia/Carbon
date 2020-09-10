package net.draycia.carbon.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.LinkedHashMap;
import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.storage.ChatUser;
import net.draycia.carbon.storage.CommandSettings;
import net.draycia.carbon.util.CarbonUtils;
import net.draycia.carbon.util.CommandUtils;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class NicknameCommand {

  @NonNull private final CarbonChat carbonChat;

  public NicknameCommand(@NonNull CarbonChat carbonChat, @NonNull CommandSettings commandSettings) {
    this.carbonChat = carbonChat;

    if (!commandSettings.isEnabled()) {
      return;
    }

    CommandUtils.handleDuplicateCommands(commandSettings);

    LinkedHashMap<String, Argument> selfArguments = new LinkedHashMap<>();
    selfArguments.put("nickname", new StringArgument());

    new CommandAPICommand(commandSettings.getName())
        .withArguments(selfArguments)
        .withAliases(commandSettings.getAliasesArray())
        .withPermission(CommandPermission.fromString("carbonchat.nickname"))
        .executesPlayer(this::executeSelf)
        .register();

    LinkedHashMap<String, Argument> otherArguments = new LinkedHashMap<>();
    otherArguments.put("player", CarbonUtils.chatUserArgument());
    otherArguments.put("nickname", new StringArgument());

    new CommandAPICommand(commandSettings.getName())
        .withArguments(otherArguments)
        .withAliases(commandSettings.getAliasesArray())
        .withPermission(CommandPermission.fromString("carbonchat.nickname.others"))
        .executes(this::executeOther)
        .register();
  }

  private void executeSelf(@NonNull Player player, @NonNull Object @NonNull [] args) {
    String nickname = (String) args[0];
    ChatUser sender = carbonChat.getUserService().wrap(player);

    if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase(player.getName())) {
      nickname = null;
    }

    sender.setNickname(nickname);

    String message;

    if (nickname == null) {
      message = carbonChat.getLanguage().getString("nickname-reset");
    } else {
      message = carbonChat.getLanguage().getString("nickname-set");
    }

    sender.sendMessage(
        carbonChat
            .getAdventureManager()
            .processMessage(
                message,
                "nickname",
                nickname == null ? "" : nickname,
                "user",
                sender.asOfflinePlayer().getName()));
  }

  private void executeOther(@NonNull CommandSender sender, @NonNull Object @NonNull [] args) {
    Audience user = carbonChat.getAdventureManager().getAudiences().audience(sender);
    ChatUser target = (ChatUser) args[0];
    String nickname = (String) args[1];

    if (nickname.equalsIgnoreCase("off")
        || nickname.equalsIgnoreCase(target.asOfflinePlayer().getName())) {
      nickname = null;
    }

    target.setNickname(nickname);

    String message;

    if (nickname == null) {
      message = carbonChat.getLanguage().getString("other-nickname-reset");
    } else {
      message = carbonChat.getLanguage().getString("other-nickname-set");
    }

    user.sendMessage(
        carbonChat
            .getAdventureManager()
            .processMessage(
                message,
                "nickname",
                nickname == null ? "" : nickname,
                "user",
                target.asOfflinePlayer().getName()));
  }
}
