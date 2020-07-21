package net.draycia.simplechat.listeners;

import net.draycia.simplechat.events.ChatFormatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class UserFormattingListener implements Listener {

    @EventHandler
    public void onFormat(ChatFormatEvent event) {
        if (!event.getUser().isOnline() || !event.getUser().asPlayer().hasPermission("simplechat.formatting")) {
            event.setFormat(event.getFormat()
                    .replace("<pre><message></pre>", "<message>")
                    .replace("<pre><message>", "<message>")
            );
        }
    }

}
