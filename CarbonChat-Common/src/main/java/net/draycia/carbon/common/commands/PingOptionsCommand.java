package net.draycia.carbon.common.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.FloatArgument;
import cloud.commandframework.context.CommandContext;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.commands.CommandSettings;
import net.draycia.carbon.api.users.CarbonUser;
import net.draycia.carbon.api.users.PlayerUser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PingOptionsCommand {

  private final @NonNull CarbonChat carbonChat;

  @SuppressWarnings("methodref.receiver.bound.invalid")
  public PingOptionsCommand(final @NonNull CommandManager<CarbonUser> commandManager) {
    this.carbonChat = CarbonChatProvider.carbonChat();

    final CommandSettings commandSettings = this.carbonChat.commandSettings().get("pingoptions");

    if (commandSettings == null || !commandSettings.enabled()) {
      return;
    }

    commandManager.command(
      commandManager.commandBuilder(commandSettings.name(), commandSettings.aliases(),
        commandManager.createDefaultCommandMeta())
        .senderType(PlayerUser.class) // player
        .permission("carbonchat.pingoptions.channels")
        .argument(StaticArgument.of("channels"))
        .argument(EnumArgument.of(Sound.Source.class, "sound"))
        .argument(FloatArgument.<CarbonUser>newBuilder("volume").withMin(0).withMax(1).asRequired().build())
        .argument(FloatArgument.<CarbonUser>newBuilder("pitch").withMin(0).withMax(1).asRequired().build())
        .handler(this::channelOptions)
        .build()
    );

    commandManager.command(
      commandManager.commandBuilder(commandSettings.name(), commandSettings.aliases(),
        commandManager.createDefaultCommandMeta())
        .senderType(PlayerUser.class) // player
        .permission("carbonchat.pingoptions.whispers")
        .argument(StaticArgument.of("whispers"))
        .argument(EnumArgument.of(Sound.Source.class, "sound"))
        .argument(FloatArgument.<CarbonUser>newBuilder("volume").withMin(0).withMax(1).asRequired().build())
        .argument(FloatArgument.<CarbonUser>newBuilder("pitch").withMin(0).withMax(1).asRequired().build())
        .handler(this::whisperOptions)
        .build()
    );
  }

  private void channelOptions(final @NonNull CommandContext<CarbonUser> context) {
    ((PlayerUser) context.getSender()).pingOptions().pingSound(Sound.sound(Key.key(context.get("sound")),
      Sound.Source.PLAYER, context.get("volume"), context.get("pitch")));

    context.getSender().sendMessage(this.carbonChat.messageProcessor().processMessage(
      this.carbonChat.translations().channelPingChanged()));
  }

  private void whisperOptions(final @NonNull CommandContext<CarbonUser> context) {
    ((PlayerUser) context.getSender()).pingOptions().whisperSound(Sound.sound(Key.key(context.get("sound")),
      Sound.Source.PLAYER, context.get("volume"), context.get("pitch")));

    context.getSender().sendMessage(this.carbonChat.messageProcessor().processMessage(
      this.carbonChat.translations().whisperPingChanged()));
  }

}
