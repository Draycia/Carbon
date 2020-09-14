package net.draycia.carbon.listeners.contexts;

import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.events.CarbonEvents;
import net.draycia.carbon.events.api.PreChatFormatEvent;
import net.draycia.carbon.util.Context;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EconomyContext {
  @NonNull
  private final CarbonChat carbonChat;

  @NonNull
  private final Economy economy;

  public EconomyContext(@NonNull final CarbonChat carbonChat) {
    this.carbonChat = carbonChat;
    this.economy = this.carbonChat.getServer().getServicesManager().getRegistration(Economy.class).getProvider();

    CarbonEvents.register(PreChatFormatEvent.class, event -> {
      final Context context = event.channel().context("vault-balance");

      if (context == null) {
        return;
      }

      final Double requiredBal;

      if (context.isNumber()) {
        requiredBal = context.asNumber().doubleValue();;
      } else {
        return;
      }

      if (requiredBal.equals(0.0)) {
        return;
      }

      final OfflinePlayer player = event.user().offlinePlayer();

      if (!this.economy.has(player, requiredBal)) {
        event.cancelled(true);

        event.user().sendMessage(this.carbonChat.adventureManager()
          .processMessage(event.channel().cannotUseMessage()));
      }
    });

    CarbonEvents.register(PreChatFormatEvent.class, event -> {
      final Context context = event.channel().context("vault-cost");

      if (context == null) {
        return;
      }

      final Double cost;

      if (context.isNumber()) {
        cost = context.asNumber().doubleValue();
      } else {
        return;
      }

      if (cost.equals(0.0)) {
        return;
      }

      final OfflinePlayer player = event.user().offlinePlayer();

      if (!this.economy.has(player, cost)) {
        event.cancelled(true);

        event.user().sendMessage(this.carbonChat.adventureManager()
          .processMessage(event.channel().cannotUseMessage()));

        return;
      }

      this.economy.withdrawPlayer(player, cost);
    });
  }

}
