package net.draycia.simplechat.storage.impl;

import co.aikar.idb.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import net.draycia.simplechat.SimpleChat;
import net.draycia.simplechat.channels.ChatChannel;
import net.draycia.simplechat.storage.ChatUser;
import net.draycia.simplechat.storage.UserChannelSettings;
import net.draycia.simplechat.storage.UserService;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MySQLUserService extends UserService {

    private final LoadingCache<UUID, SimpleChatUser> userCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .removalListener(this::saveUser)
            .build(CacheLoader.from(this::loadUser));

    private SimpleChat simpleChat;
    private Database database;

    private MySQLUserService() { }

    public MySQLUserService(SimpleChat simpleChat) {
        this.simpleChat = simpleChat;

        ConfigurationSection section = simpleChat.getConfig().getConfigurationSection("storage");

        String username = section.getString("username");
        String password = section.getString("password");
        String dbname = section.getString("database");
        String hostandport = section.getString("hostname") + ":" + section.getString("port");

        database = new HikariPooledDatabase(BukkitDB.getRecommendedOptions(simpleChat, username, password, dbname, hostandport));

        DB.setGlobalDatabase(database);

        try {
            database.executeUpdate("CREATE TABLE IF NOT EXISTS sc_users (uuid CHAR(36) PRIMARY KEY," +
                    "channel VARCHAR(16), muted BOOLEAN, shadowmuted BOOLEAN)");

            database.executeUpdate("CREATE TABLE IF NOT EXISTS sc_channel_settings (uuid CHAR(36), channel CHAR(16), spying BOOLEAN, ignored BOOLEAN, color TINYTEXT, PRIMARY KEY (uuid, channel))");

            database.executeUpdate("CREATE TABLE IF NOT EXISTS sc_ignored_users (uuid CHAR(36), user CHAR(36), PRIMARY KEY (uuid, user))");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(simpleChat, userCache::cleanUp, 0L, 20 * 60 * 10);
    }

    @Override
    public ChatUser wrap(OfflinePlayer player) {
        return wrap(player.getUniqueId());
    }

    @Override
    public ChatUser wrap(UUID uuid) {
        try {
            return userCache.get(uuid);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void cleanUp() {
        userCache.invalidateAll();
        userCache.cleanUp();
        database.close();
    }

    private SimpleChatUser loadUser(UUID uuid) {
        SimpleChatUser user = new SimpleChatUser(uuid);


        try (DbStatement statement = database.query("SELECT * from sc_users WHERE uuid = ?;")) {
            statement.execute(uuid.toString());

            DbRow users = statement.getNextRow();

            if (users == null) {
                return user;
            }

            List<DbRow> ignoredChannels = database.getResults("SELECT * from sc_channel_settings WHERE uuid = ?;", uuid.toString());
            List<DbRow> ignoredUsers = database.getResults("SELECT * from sc_ignored_users WHERE uuid = ?;", uuid.toString());

            user.setSelectedChannel(simpleChat.getChannel(users.getString("channel")));
            user.setMuted(users.<Boolean>get("muted"));
            user.setShadowMuted(users.<Boolean>get("shadowmuted"));

            for (DbRow ignoredChannel : ignoredChannels) {
                ChatChannel chatChannel = simpleChat.getChannel(ignoredChannel.getString("channel"));
                UserChannelSettings settings = user.getChannelSettings(chatChannel);

                settings.setSpying(ignoredChannel.<Boolean>get("spying"));
                settings.setIgnoring(ignoredChannel.<Boolean>get("ignored"));
                settings.setColor(TextColor.fromHexString(ignoredChannel.getString("color")));
            }

            for (DbRow ignoredUser : ignoredUsers) {
                user.setIgnoringUser(UUID.fromString(ignoredUser.getString("user")), true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    private void saveUser(RemovalNotification<UUID, SimpleChatUser> notification) {
        SimpleChatUser user = notification.getValue();

        if (user == null) {
            System.out.println("null");
            return;
        }

        database.createTransaction(stm -> {
            // Save user general data
            String selectedName = null;

            if (user.getSelectedChannel() != null) {
                selectedName = user.getSelectedChannel().getName();
            }

            simpleChat.getLogger().info("Saving user data!");
            stm.executeUpdateQuery("INSERT INTO sc_users (uuid, channel, muted, shadowmuted) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE channel = ?, muted = ?, shadowmuted = ?",
                    user.getUUID().toString(), selectedName, user.isMuted(), user.isShadowMuted(),
                    selectedName, user.isMuted(), user.isShadowMuted());

            simpleChat.getLogger().info("Saving user channel settings!");
            // Save user channel settings
            for (Map.Entry<String, UserChannelSettings> entry : user.getChannelSettings().entrySet()) {
                UserChannelSettings value = entry.getValue();

                String colorString = null;

                if (value.getColor() != null) {
                    colorString = value.getColor().asHexString();
                }

                stm.executeUpdateQuery("INSERT INTO sc_channel_settings (uuid, channel, spying, ignored, color) VALUES (?, ?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE spying = ?, ignored = ?, color = ?",
                        user.getUUID().toString(), entry.getKey(), value.isSpying(), value.isIgnored(), colorString,
                        value.isSpying(), value.isIgnored(), colorString);
            }

            simpleChat.getLogger().info("Saving user ignores!");
            // Save user ignore list (remove old entries then add new ones)
            // TODO: keep DB up to date with settings as settings are mutated
            stm.executeUpdateQuery("DELETE FROM sc_ignored_users WHERE uuid = ?", user.getUUID().toString());

            for (UUID entry : user.getIgnoredUsers()) {
                stm.executeUpdateQuery("INSERT INTO sc_ignored_users (uuid, user) VALUES (?, ?)",
                        user.getUUID().toString(), entry.toString());
            }

            return true;
        });
    }

}
