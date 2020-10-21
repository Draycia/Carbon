package net.draycia.carbon.api.events;

import net.draycia.carbon.api.events.misc.CarbonEvent;
import net.draycia.carbon.api.users.PlayerUser;
import net.kyori.event.Cancellable;
import org.checkerframework.checker.nullness.qual.NonNull;

public class UserEvent implements CarbonEvent, Cancellable {

  private final @NonNull PlayerUser user;
  private boolean cancelled = false;

  public UserEvent(final @NonNull PlayerUser user) {
    this.user = user;
  }

  public @NonNull PlayerUser user() {
    return this.user;
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(final boolean cancelled) {
    this.cancelled = cancelled;
  }

  public static class Join extends UserEvent {
    public Join(final @NonNull PlayerUser user) {
      super(user);
    }
  }

  public static class Leave extends UserEvent {
    public Leave(final @NonNull PlayerUser user) {
      super(user);
    }
  }

}
