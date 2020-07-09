package net.draycia.simplechat.listeners;

import net.draycia.simplechat.events.ChatFormatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class OfflineNameHandler implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOfflineMessage(ChatFormatEvent event) {
        // If the player isn't online (cross server message), use their normal name
        if (!event.getUser().isOnline()) {
            event.setFormat(event.getFormat().replace("%player_displayname%", "%player_name%"));
        }
    }

}
