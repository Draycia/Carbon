package net.draycia.carbon.listeners.events;

import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.storage.ChatUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerJoinListener implements Listener {

  @NonNull
  private final CarbonChat carbonChat;

  public PlayerJoinListener(@NonNull final CarbonChat carbonChat) {
    this.carbonChat = carbonChat;
  }

  @EventHandler
  public void onPlayerJoin(final PlayerJoinEvent event) {
    final ChatUser user = this.carbonChat.userService().wrap(event.getPlayer());

    this.carbonChat.userService().validate(user);

    if (user.nickname() != null) {
      user.nickname(user.nickname());
    }

    final String channel = this.carbonChat.getConfig().getString("channel-on-join");

    if (channel == null || channel.isEmpty()) {
      return;
    }

    if (channel.equals("DEFAULT")) {
      user.selectedChannel(this.carbonChat.channelManager().defaultChannel());
      return;
    }

    final ChatChannel chatChannel = this.carbonChat.channelManager().registry().channel(channel);

    if (chatChannel != null) {
      user.selectedChannel(chatChannel);
    }
  }

  @EventHandler
  public void onPlayerQuit(final PlayerQuitEvent event) {
    final ChatUser user = this.carbonChat.userService().wrapIfLoaded(event.getPlayer());

    if (user != null) {
      this.carbonChat.userService().invalidate(user);
    }
  }

}
