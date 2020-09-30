package net.draycia.carbon.listeners.events;

import net.draycia.carbon.util.CarbonUtils;
import net.draycia.carbon.api.events.misc.CarbonEvents;
import net.draycia.carbon.api.events.ChatComponentEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.event.PostOrders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ItemLinkHandler {

  public ItemLinkHandler() {
    CarbonEvents.register(ChatComponentEvent.class, PostOrders.FIRST, false, event -> {
      // Handle item linking placeholders
      final Player player = Bukkit.getPlayer(event.sender().uuid());

      if (player == null) {
        return;
      }

      if (!player.hasPermission("carbonchat.itemlink")) {
        return;
      }

      final Component itemComponent = CarbonUtils.createComponent(player);

      if (itemComponent.equals(TextComponent.empty())) {
        return;
      }

      for (final Pattern pattern : event.channel().itemLinkPatterns()) {
        final String patternContent = pattern.toString().replace("\\Q", "")
          .replace("\\E", "");

        if (event.originalMessage().contains(patternContent)) {
          final TextComponent component = (TextComponent) event.component()
            .replaceFirstText(pattern, input -> itemComponent);

          event.component(component);
          break;
        }
      }
    });
  }

}
