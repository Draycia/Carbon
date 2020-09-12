package net.draycia.carbon.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.draycia.carbon.events.impls.ChatFormatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RelationalPlaceholderHandler implements Listener {

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPapiPlaceholder(final ChatFormatEvent event) {
    if (event.target() == null) {
      return;
    }

    if (!event.sender().online() || !event.target().online()) {
      return;
    }

    final Player sender = event.sender().player();
    final Player target = event.target().player();

    event.format(PlaceholderAPI.setRelationalPlaceholders(sender, target, event.format()));
  }

}
