package net.draycia.carbon.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import java.util.LinkedHashMap;
import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.storage.ChatUser;
import net.draycia.carbon.storage.CommandSettings;
import net.draycia.carbon.storage.UserChannelSettings;
import net.draycia.carbon.util.CarbonUtils;
import net.draycia.carbon.util.CommandUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ToggleCommand {

  @NonNull private final CarbonChat carbonChat;

  public ToggleCommand(@NonNull CarbonChat carbonChat, @NonNull CommandSettings commandSettings) {
    this.carbonChat = carbonChat;

    if (!commandSettings.isEnabled()) {
      return;
    }

    CommandUtils.handleDuplicateCommands(commandSettings);

    LinkedHashMap<String, Argument> channelArguments = new LinkedHashMap<>();
    channelArguments.put("channel", CarbonUtils.channelArgument());

    new CommandAPICommand(commandSettings.getName())
        .withArguments(channelArguments)
        .withAliases(commandSettings.getAliasesArray())
        .withPermission(CommandPermission.fromString("carbonchat.toggle"))
        .executesPlayer(this::executeSelf)
        .register();

    LinkedHashMap<String, Argument> argumentsOther = new LinkedHashMap<>();
    argumentsOther.put("players", CarbonUtils.chatUserArgument());
    argumentsOther.put("channel", CarbonUtils.channelArgument());

    new CommandAPICommand(commandSettings.getName())
        .withArguments(argumentsOther)
        .withAliases(commandSettings.getAliasesArray())
        .withPermission(CommandPermission.fromString("carbonchat.toggle"))
        .executes(this::executeOther)
        .register();
  }

  private void executeSelf(@NonNull Player player, @NonNull Object @NonNull [] args) {
    ChatUser user = carbonChat.getUserService().wrap(player);
    ChatChannel channel = (ChatChannel) args[0];

    String message;

    UserChannelSettings settings = user.getChannelSettings(channel);

    if (!channel.isIgnorable()) {
      message = channel.getCannotIgnoreMessage();
    } else if (settings.isIgnored()) {
      settings.setIgnoring(false);
      message = channel.getToggleOffMessage();
    } else {
      settings.setIgnoring(true);
      message = channel.getToggleOnMessage();
    }

    user.sendMessage(
        carbonChat
            .getAdventureManager()
            .processMessageWithPapi(
                player,
                message,
                "br",
                "\n",
                "color",
                "<color:" + channel.getChannelColor(user).toString() + ">",
                "channel",
                channel.getName()));
  }

  private void executeOther(@NonNull CommandSender sender, @NonNull Object @NonNull [] args) {
    ChatUser user = (ChatUser) args[0];
    ChatChannel channel = (ChatChannel) args[1];

    String message;
    String otherMessage;

    UserChannelSettings settings = user.getChannelSettings(channel);

    if (settings.isIgnored()) {
      settings.setIgnoring(false);
      message = channel.getToggleOffMessage();
      otherMessage = channel.getToggleOtherOffMessage();
    } else {
      settings.setIgnoring(true);
      message = channel.getToggleOnMessage();
      otherMessage = channel.getToggleOtherOnMessage();
    }

    user.sendMessage(
        carbonChat
            .getAdventureManager()
            .processMessage(
                message,
                "br",
                "\n",
                "color",
                "<color:" + channel.getChannelColor(user).toString() + ">",
                "channel",
                channel.getName()));

    carbonChat
        .getAdventureManager()
        .getAudiences()
        .audience(sender)
        .sendMessage(
            carbonChat
                .getAdventureManager()
                .processMessage(
                    otherMessage,
                    "br",
                    "\n",
                    "color",
                    "<color:" + channel.getChannelColor(user).toString() + ">",
                    "channel",
                    channel.getName(),
                    "player",
                    user.asOfflinePlayer().getName()));
  }
}
