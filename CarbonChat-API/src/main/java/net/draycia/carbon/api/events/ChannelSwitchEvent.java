package net.draycia.carbon.api.events;

import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.events.misc.CarbonEvent;
import net.draycia.carbon.api.users.PlayerUser;
import net.kyori.event.Cancellable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ChannelSwitchEvent implements CarbonEvent, Cancellable {

  private final @NonNull ChatChannel channel;
  private final @NonNull PlayerUser user;
  private @NonNull String failureMessage;
  private boolean cancelled = false;

  public ChannelSwitchEvent(final @NonNull ChatChannel channel, final @NonNull PlayerUser user, final @NonNull String failureMessage) {
    this.channel = channel;
    this.user = user;
    this.failureMessage = failureMessage;
  }

  public @NonNull PlayerUser user() {
    return this.user;
  }

  public @NonNull ChatChannel channel() {
    return this.channel;
  }

  public @NonNull String failureMessage() {
    return this.failureMessage;
  }

  public void failureMessage(final @NonNull String failureMessage) {
    this.failureMessage = failureMessage;
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(final boolean cancelled) {
    this.cancelled = cancelled;
  }

}
