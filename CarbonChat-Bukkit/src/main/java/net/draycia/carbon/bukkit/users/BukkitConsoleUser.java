package net.draycia.carbon.bukkit.users;

import net.draycia.carbon.api.users.ConsoleUser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BukkitConsoleUser implements ConsoleUser, ForwardingAudience.Single {

  private final @NonNull Audience audience;

  public BukkitConsoleUser(final @NonNull ConsoleCommandSender sender) {
    final Plugin plugin = Bukkit.getPluginManager().getPlugin("CarbonChat");

    this.audience = BukkitAudiences.create(plugin).sender(sender);
  }

  public BukkitConsoleUser() {
    this(Bukkit.getConsoleSender());
  }

  @Override
  public @NonNull Identity identity() {
    return Identity.nil();
  }

  @Override
  public @NonNull Audience audience() {
    return this.audience;
  }

  @Override
  public boolean hasPermission(final @NonNull String permission) {
    return true;
  }

  @Override
  public @NonNull String name() {
    return "Console";
  }

}
