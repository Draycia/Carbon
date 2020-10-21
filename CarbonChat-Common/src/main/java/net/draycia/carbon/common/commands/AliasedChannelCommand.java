package net.draycia.carbon.common.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.channels.TextChannel;
import net.draycia.carbon.api.users.CarbonUser;
import net.draycia.carbon.api.users.PlayerUser;
import net.kyori.adventure.identity.Identity;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AliasedChannelCommand {

  private final @NonNull CarbonChat carbonChat;

  private final @NonNull TextChannel chatChannel;

  private final @NonNull String commandName;

  public AliasedChannelCommand(final @NonNull CommandManager<CarbonUser> commandManager, final @NonNull TextChannel chatChannel) {
    this.carbonChat = CarbonChatProvider.carbonChat();
    this.chatChannel = chatChannel;
    this.commandName = chatChannel.key();

    commandManager.command(
      commandManager.commandBuilder(this.commandName,
        commandManager.createDefaultCommandMeta())
        .senderType(PlayerUser.class) // player
        .permission("carbonchat.channel")
        .handler(this::channel)
        .build()
    );

    commandManager.command(
      commandManager.commandBuilder(this.commandName,
        commandManager.createDefaultCommandMeta())
        .senderType(PlayerUser.class) // player
        .permission("carbonchat.channel.message")
        .argument(StringArgument.<CarbonUser>newBuilder("message").greedy().build())
        .handler(this::sendMessage)
        .build()
    );
  }

  private void channel(final @NonNull CommandContext<CarbonUser> context) {
    final PlayerUser user = (PlayerUser) context.getSender();

    if (user.channelSettings(this.chatChannel()).ignored()) {
      user.sendMessage(Identity.nil(), this.carbonChat.messageProcessor().processMessage(this.chatChannel().cannotUseMessage(),
        "color", "<" + this.chatChannel().channelColor(user).toString() + ">",
        "channel", this.chatChannel().name()));

      return;
    }

    user.selectedChannel(this.chatChannel());
  }

  private void sendMessage(final @NonNull CommandContext<CarbonUser> context) {
    context.<String>getOptional("message").ifPresent(message -> {
      this.chatChannel().sendComponentsAndLog(
        context.getSender().identity(),
        this.chatChannel().parseMessage((PlayerUser) context.getSender(), message, false));
    });
  }

  public @NonNull TextChannel chatChannel() {
    return this.chatChannel;
  }

  public @NonNull String commandName() {
    return this.commandName;
  }
}
