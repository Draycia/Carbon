package net.draycia.carbon.api.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.configurate.objectmapping.Setting;
import org.spongepowered.configurate.serialize.ConfigSerializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSerializable
public class CarbonSettings {

  private static final ObjectMapper<CarbonSettings> MAPPER;

  static {
    try {
      MAPPER = ObjectMapper.forClass(CarbonSettings.class); // We hold on to the instance of our ObjectMapper
    } catch (final ObjectMappingException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static CarbonSettings loadFrom(final CommentedConfigurationNode node) throws ObjectMappingException {
    return MAPPER.bindToNew().populate(node);
  }

  public void saveTo(final CommentedConfigurationNode node) throws ObjectMappingException {
    MAPPER.bind(this).serialize(node);
  }

  @Setting(comment = "The prefix shown when spying on a user's message")
  private @NonNull String spyPrefix = "<color>[SPY] ";

  @Setting(comment = "Used for <server> placeholder in channel formats")
  private @NonNull String serverName = "Server";

  @Setting(comment = "Set this to false to disable warnings and tips when the plugin thinks there may be configuration issues.")
  private boolean showTips = true;

  @Setting(comment = "Options: JSON, MYSQL")
  private @Nullable StorageType storageType = StorageType.JSON;

  @Setting(comment = "The credentials used to connect to your database. Requires storageType=MYSQL")
  private @NonNull SQLCredentials sqlCredentials = new SQLCredentials();

  @Setting(comment = "Options: NONE, REDIS, BUNGEECORD")
  private @NonNull MessagingType messagingType = MessagingType.NONE;

  @Setting(comment = " Options:\n" +
    " 'bungee' - Uses bungee plugin messaging, requires BungeeCord or another proxy which supports it (Velocity!)\n" +
    " 'redis' - Uses redis for cross server syncing, does not require a server proxy\n" +
    " 'none' - Do not sync anything cross server, this is the default\n" +
    "\n" +
    " Note: In order for channels to sync cross server, you'll need to enable is-cross-server for the\n" +
    "   channel as well as this setting.")
  private @NonNull RedisCredentials redisCredentials = new RedisCredentials();

  @Setting(comment = "Used for message formats - custom tags that are replaced in descending order" +
    "\nFor example, in the default config, <group> wil be replaced with <green>%vault_group%")
  private @NonNull Map<@Nullable String, @NonNull String> customPlaceholders = new HashMap<String, String>() {
    {
      this.put("prefix", "<gray>[<group><gray>]");
      this.put("group", "<green>%vault_group%");
    }
  };

  @Setting(comment = "Plays a sound and highlights the message when someone types your name")
  private @NonNull ChannelPings channelPings = new ChannelPings();

  @Setting(comment = "")
  private @NonNull WhisperOptions whisperOptions = new WhisperOptions();

  @Setting(comment = "Sets the player's channel to the specified channel when they join" +
    "\nSet to \"\" or remove to disable" +
    "\nSet to DEFAULT to set the player's channel to the default channel on join" +
    "\nOtherwise, set to a channel to set the player's channel to it on join" +
    "\nFor example, channel-on-join: \"global\" sets their channel to global on join")
  private @Nullable String channelOnJoin = "";

  @Setting private List<Pattern> itemLinkPatterns =
    Collections.singletonList(Pattern.compile(Pattern.quote("[item]")));

  @NonNull
  public List<@NonNull Pattern> itemLinkPatterns() {
    return this.itemLinkPatterns;
  }

  public @NonNull String spyPrefix() {
    return this.spyPrefix;
  }

  public @NonNull String serverName() {
    return this.serverName;
  }

  public boolean showTips() {
    return this.showTips;
  }

  public @Nullable StorageType storageType() {
    return this.storageType;
  }

  public SQLCredentials sqlCredentials() {
    return this.sqlCredentials;
  }

  public @NonNull MessagingType messagingType() {
    return this.messagingType;
  }

  public @NonNull RedisCredentials redisCredentials() {
    return this.redisCredentials;
  }

  public Map<String, String> customPlaceholders() {
    return this.customPlaceholders;
  }

  public @Nullable String channelOnJoin() {
    return this.channelOnJoin;
  }

  public @NonNull ChannelPings channelPings() {
    return this.channelPings;
  }

  public @NonNull WhisperOptions whisperOptions() {
    return this.whisperOptions;
  }

}
