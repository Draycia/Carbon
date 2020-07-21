package net.draycia.simplechat.listeners;

import net.draycia.simplechat.SimpleChat;
import net.draycia.simplechat.events.ChatComponentEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

public class ItemLinkHandler implements Listener {

    private final SimpleChat simpleChat;

    public ItemLinkHandler(SimpleChat simpleChat) {
        this.simpleChat = simpleChat;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemLink(ChatComponentEvent event) {
        // Handle item linking placeholders
        if (event.getUser().isOnline()) {
            for (Pattern pattern : event.getChatChannel().getItemLinkPatterns()) {
                String patternContent = pattern.toString().replace("\\Q", "").replace("\\E", "");

                if (event.getOriginalMessage().contains(patternContent)) {
                    TextComponent component = event.getComponent().replace(pattern, (input) -> {
                        return TextComponent.builder().append(simpleChat.getItemStackUtils().createComponent(event.getUser().asPlayer()));
                    });

                    event.setComponent(component);
                    break;
                }
            }
        }
    }

}
