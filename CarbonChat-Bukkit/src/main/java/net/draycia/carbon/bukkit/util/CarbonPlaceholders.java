package net.draycia.carbon.bukkit.util;

import net.draycia.carbon.api.channels.ChatChannel;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.draycia.carbon.bukkit.CarbonChatBukkit;
import net.draycia.carbon.api.users.PlayerUser;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CarbonPlaceholders extends PlaceholderExpansion {

  private final @NonNull CarbonChatBukkit carbonChat;

  public CarbonPlaceholders(final @NonNull CarbonChatBukkit carbonChat) {
    this.carbonChat = carbonChat;
  }

  @Override
  public @NonNull String getIdentifier() {
    return "carbonchat";
  }

  @Override
  public @NonNull String getAuthor() {
    return "Draycia (Vicarious#0001)";
  }

  @Override
  public @NonNull String getVersion() {
    return "1.0.0";
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @NonNull String onPlaceholderRequest(final @NonNull Player player, final @NonNull String identifier) {
    final String key = identifier.toLowerCase();

    if (key.startsWith("can_use_")) {
      final String value = key.replace("can_use_", "");

      final ChatChannel channel = this.carbonChat.channelRegistry().get(value);

      if (channel == null) {
        return "false";
      }

      final PlayerUser user = this.carbonChat.userService().wrap(player.getUniqueId());

      return String.valueOf(channel.canPlayerUse(user));
    } else if (key.startsWith("can_see_")) {
      final String value = key.replace("can_see_", "");

      final ChatChannel channel = this.carbonChat.channelRegistry().get(value);

      if (channel == null) {
        return "false";
      }

      final PlayerUser user = this.carbonChat.userService().wrap(player.getUniqueId());

      return String.valueOf(channel.canPlayerSee(user, true));
    } else if (key.startsWith("ignoring_channel_")) {
      final String value = key.replace("ignoring_channel_", "");

      final ChatChannel channel = this.carbonChat.channelRegistry().get(value);

      if (channel == null) {
        return "false";
      }

      final PlayerUser user = this.carbonChat.userService().wrap(player.getUniqueId());

      return String.valueOf(user.channelSettings(channel).ignored());
    } else if (key.startsWith("selected_channel")) {
      final ChatChannel channel = this.carbonChat.userService().wrap(player.getUniqueId()).selectedChannel();

      return channel == null ? this.carbonChat.channelRegistry().defaultValue().name() : channel.name();
    }

    return "";
  }

}
