package net.draycia.carbon.storage.impl;

import co.aikar.idb.*;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.storage.ChatUser;
import net.draycia.carbon.storage.UserChannelSettings;
import net.draycia.carbon.storage.UserService;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MySQLUserService implements UserService {

  @NonNull private final CarbonChat carbonChat;
  @NonNull private final Database database;

  @NonNull
  private final LoadingCache<@NonNull UUID, @NonNull CarbonChatUser> userCache =
      CacheBuilder.newBuilder()
          .removalListener(this::saveUser)
          .build(CacheLoader.from(this::loadUser));

  public MySQLUserService(@NonNull CarbonChat carbonChat) {
    this.carbonChat = carbonChat;

    ConfigurationSection section = carbonChat.getConfig().getConfigurationSection("storage");

    if (section == null) {
      throw new IllegalStateException("Missing Database Credentials!");
    }

    String username = section.getString("username", "username");
    String password = section.getString("password", "password");
    String dbname = section.getString("database", "0");
    String hostandport =
        section.getString("hostname", "hostname") + ":" + section.getString("port", "0");

    database =
        new HikariPooledDatabase(
            BukkitDB.getRecommendedOptions(carbonChat, username, password, dbname, hostandport));

    DB.setGlobalDatabase(database);

    try {
      database.executeUpdate(
          "CREATE TABLE IF NOT EXISTS sc_users (uuid CHAR(36) PRIMARY KEY,"
              + "channel VARCHAR(16), muted BOOLEAN, shadowmuted BOOLEAN, spyingwhispers BOOLEAN,"
              + "nickname VARCHAR(512))");

      // Ignore the exception, it's just saying the column already exists
      try {
        database.executeUpdate(
            "ALTER TABLE sc_users ADD COLUMN spyingwhispers BOOLEAN DEFAULT false");
        database.executeUpdate(
            "ALTER TABLE sc_users ADD COLUMN nickname VARCHAR(512) DEFAULT false");
      } catch (SQLSyntaxErrorException ignored) {
      }

      database.executeUpdate(
          "CREATE TABLE IF NOT EXISTS sc_channel_settings (uuid CHAR(36), channel CHAR(16), spying BOOLEAN, ignored BOOLEAN, color TINYTEXT, PRIMARY KEY (uuid, channel))");

      database.executeUpdate(
          "CREATE TABLE IF NOT EXISTS sc_ignored_users (uuid CHAR(36), user CHAR(36), PRIMARY KEY (uuid, user))");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDisable() {
    userCache.invalidateAll();
    userCache.cleanUp();
    database.close();
  }

  @Override
  @Nullable
  public ChatUser wrap(@NonNull String name) {
    Player player = Bukkit.getPlayer(name);

    if (player != null) {
      return wrap(player);
    }

    return wrap(Bukkit.getOfflinePlayer(name));
  }

  @Override
  @Nullable
  public ChatUser wrap(@NonNull OfflinePlayer player) {
    return wrap(player.getUniqueId());
  }

  @Override
  @Nullable
  public ChatUser wrap(@NonNull UUID uuid) {
    try {
      return userCache.get(uuid);
    } catch (ExecutionException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  @Nullable
  public ChatUser wrapIfLoaded(@NonNull UUID uuid) {
    return userCache.getIfPresent(uuid);
  }

  @Override
  @Nullable
  public ChatUser refreshUser(@NonNull UUID uuid) {
    userCache.invalidate(uuid);

    return this.wrap(uuid);
  }

  @Override
  public void invalidate(@NonNull ChatUser user) {
    userCache.invalidate(user.getUUID());
  }

  @Override
  public void validate(@NonNull ChatUser user) {
    userCache.put(user.getUUID(), (CarbonChatUser) user);
  }

  @NonNull
  private CarbonChatUser loadUser(@NonNull UUID uuid) {
    CarbonChatUser user = new CarbonChatUser(uuid);

    try (DbStatement statement = database.query("SELECT * from sc_users WHERE uuid = ?;")) {
      statement.execute(uuid.toString());

      DbRow users = statement.getNextRow();

      if (users == null) {
        return user;
      }

      List<DbRow> channelSettings =
          database.getResults("SELECT * from sc_channel_settings WHERE uuid = ?;", uuid.toString());
      List<DbRow> ignoredUsers =
          database.getResults("SELECT * from sc_ignored_users WHERE uuid = ?;", uuid.toString());

      ChatChannel channel =
          carbonChat.getChannelManager().getChannelOrDefault(users.getString("channel"));

      if (channel != null) {
        user.setSelectedChannel(channel, true);
      }

      String nickname = users.getString("nickname");

      if (nickname != null) {
        user.setNickname(nickname, true);
      }

      user.setMuted(users.<Boolean>get("muted"), true);
      user.setShadowMuted(users.<Boolean>get("shadowmuted"), true);
      user.setSpyingWhispers(users.<Boolean>get("spyingwhispers"), true);

      for (DbRow channelSetting : channelSettings) {
        ChatChannel chatChannel =
            carbonChat.getChannelManager().getRegistry().get(channelSetting.getString("channel"));

        if (chatChannel != null) {
          UserChannelSettings settings = user.getChannelSettings(chatChannel);

          settings.setSpying(channelSetting.<Boolean>get("spying"), true);
          settings.setIgnoring(channelSetting.<Boolean>get("ignored"), true);

          String color = channelSetting.getString("color");

          if (color != null) {
            settings.setColor(TextColor.fromHexString(color), true);
          }
        }
      }

      for (DbRow ignoredUser : ignoredUsers) {
        user.setIgnoringUser(UUID.fromString(ignoredUser.getString("user")), true, true);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return user;
  }

  private void saveUser(
      @NonNull RemovalNotification<@NonNull UUID, @NonNull CarbonChatUser> notification) {
    CarbonChatUser user = notification.getValue();

    database.createTransaction(
        stm -> {
          // Save user general data
          String selectedName = null;

          if (user.getSelectedChannel() != null) {
            selectedName = user.getSelectedChannel().getKey();
          }

          carbonChat.getLogger().info("Saving user data!");
          stm.executeUpdateQuery(
              "INSERT INTO sc_users (uuid, channel, muted, shadowmuted, spyingwhispers, nickname) VALUES (?, ?, ?, ?, ?, ?) "
                  + "ON DUPLICATE KEY UPDATE channel = ?, muted = ?, shadowmuted = ?, spyingwhispers = ?, nickname =?",
              user.getUUID().toString(),
              selectedName,
              user.isMuted(),
              user.isShadowMuted(),
              user.isSpyingWhispers(),
              user.getNickname(),
              selectedName,
              user.isMuted(),
              user.isShadowMuted(),
              user.isSpyingWhispers(),
              user.getNickname());

          carbonChat.getLogger().info("Saving user channel settings!");
          // Save user channel settings
          for (Map.Entry<String, ? extends UserChannelSettings> entry :
              user.getChannelSettings().entrySet()) {
            UserChannelSettings value = entry.getValue();

            String colorString = null;

            if (value.getColor() != null) {
              colorString = value.getColor().asHexString();
            }

            stm.executeUpdateQuery(
                "INSERT INTO sc_channel_settings (uuid, channel, spying, ignored, color) VALUES (?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE spying = ?, ignored = ?, color = ?",
                user.getUUID().toString(),
                entry.getKey(),
                value.isSpying(),
                value.isIgnored(),
                colorString,
                value.isSpying(),
                value.isIgnored(),
                colorString);
          }

          carbonChat.getLogger().info("Saving user ignores!");
          // Save user ignore list (remove old entries then add new ones)
          // TODO: keep DB up to date with settings as settings are mutated
          stm.executeUpdateQuery(
              "DELETE FROM sc_ignored_users WHERE uuid = ?", user.getUUID().toString());

          for (UUID entry : user.getIgnoredUsers()) {
            stm.executeUpdateQuery(
                "INSERT INTO sc_ignored_users (uuid, user) VALUES (?, ?)",
                user.getUUID().toString(),
                entry.toString());
          }

          return true;
        });
  }
}
