package net.draycia.carbon.common.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.users.CarbonUser;
import net.draycia.carbon.api.commands.CommandSettings;
import net.draycia.carbon.api.users.PlayerUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Template;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MeCommand {

  private final @NonNull CarbonChat carbonChat;

  @SuppressWarnings("methodref.receiver.bound.invalid")
  public MeCommand(final @NonNull CommandManager<CarbonUser> commandManager) {
    this.carbonChat = CarbonChatProvider.carbonChat();

    final CommandSettings commandSettings = this.carbonChat.commandSettings().get("me");

    if (commandSettings == null || !commandSettings.enabled()) {
      return;
    }

    commandManager.command(
      commandManager.commandBuilder(commandSettings.name(), commandSettings.aliases(),
        commandManager.createDefaultCommandMeta())
        .senderType(PlayerUser.class) // player
        .permission("carbonchat.me")
        .argument(StringArgument.<CarbonUser>newBuilder("message").greedy().build())
        .handler(this::message)
        .build()
    );
  }

  private void message(final @NonNull CommandContext<CarbonUser> context) {
    final PlayerUser user = (PlayerUser) context.getSender();

    final String message = context.<String>get("message").replace("</pre>", "");
    String format = this.carbonChat.translations().roleplayFormat();

    if (!user.hasPermission("carbonchat.me.formatting")) {
      format = format.replace("<message>", "<pre><message></pre>");
    }

    final Component component = this.carbonChat.messageProcessor().processMessage(format,
      Template.of("displayname", user.displayName()), Template.of("message", message));

    if (user.shadowMuted()) {
      user.sendMessage(user.identity(), component);
    } else {
      for (final PlayerUser onlineUser : this.carbonChat.userService().onlineUsers()) {
        if (onlineUser.ignoringUser(user)) {
          continue;
        }

        onlineUser.sendMessage(user.identity(), component);
      }
    }
  }

}
