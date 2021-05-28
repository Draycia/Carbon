package net.draycia.carbon.bukkit.users;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.users.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class should not be used.
 * No data is persisted or saved.
 * This exists merely as a placeholder.
 */
@Singleton
public final class MemoryUserManagerBukkit implements UserManager {

    private final @NonNull Map<UUID, @NonNull CarbonPlayer> users = new HashMap<>();

    @Override
    public @Nullable CarbonPlayer carbonPlayer(final @NonNull UUID uuid) {
        if (this.users.containsKey(uuid)) {
            return this.users.get(uuid);
        }

        final Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return null;
        }

        return this.users.computeIfAbsent(player.getUniqueId(), key ->
            new CarbonPlayerBukkit(player.getName(), player.getUniqueId()));
    }

    @Override
    public @Nullable CarbonPlayer carbonPlayer(final @NonNull String username) {
        final Player player = Bukkit.getPlayer(username);

        if (player == null) {
            return null;
        }

        return this.carbonPlayer(player.getUniqueId());
    }

}
