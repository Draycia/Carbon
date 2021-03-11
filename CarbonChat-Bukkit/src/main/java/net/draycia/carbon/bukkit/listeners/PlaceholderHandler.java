package net.draycia.carbon.bukkit.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.draycia.carbon.api.events.misc.CarbonEvents;
import net.draycia.carbon.api.events.PreChatFormatEvent;
import net.kyori.event.PostOrders;
import org.bukkit.Bukkit;

public class PlaceholderHandler {

  public PlaceholderHandler() {
    CarbonEvents.register(PreChatFormatEvent.class, PostOrders.FIRST, false, event -> {
      event.format(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(event.sender().uuid()), event.format()));
    });
  }

}
