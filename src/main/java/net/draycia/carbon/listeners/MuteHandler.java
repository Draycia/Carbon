package net.draycia.carbon.listeners;

import net.draycia.carbon.events.impls.PreChatFormatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MuteHandler implements Listener {

  @EventHandler
  public void onMute(final PreChatFormatEvent event) {
    if (event.user().muted()) {
      event.setCancelled(true);
    }
  }

}
