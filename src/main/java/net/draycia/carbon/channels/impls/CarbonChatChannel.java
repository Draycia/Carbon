package net.draycia.carbon.channels.impls;

import net.draycia.carbon.CarbonChat;
import net.draycia.carbon.channels.ChatChannel;
import net.draycia.carbon.events.CarbonEvents;
import net.draycia.carbon.events.api.ChatComponentEvent;
import net.draycia.carbon.events.api.ChatFormatEvent;
import net.draycia.carbon.events.api.PreChatFormatEvent;
import net.draycia.carbon.storage.ChatUser;
import net.draycia.carbon.util.CarbonUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class CarbonChatChannel extends ChatChannel {

  @NonNull
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("<message>");

  @NonNull
  private final String key;

  @NonNull
  private final CarbonChat carbonChat;

  @Nullable
  private final ConfigurationSection config;

  public CarbonChatChannel(@NonNull final String key, @NonNull final CarbonChat carbonChat, @Nullable final ConfigurationSection config) {
    this.key = key;
    this.carbonChat = carbonChat;
    this.config = config;
  }

  @NonNull
  public CarbonChat carbonChat() {
    return this.carbonChat;
  }

  @Override
  public boolean testContext(@NonNull final ChatUser sender, @NonNull final ChatUser target) {
    return this.carbonChat.contextManager().testContext(sender, target, this);
  }

  @Override
  public boolean canPlayerUse(@NonNull final ChatUser user) {
    return user.player().hasPermission("carbonchat.channels." + this.name() + ".use");
  }

  @Override
  @NonNull
  public List<@NonNull ChatUser> audiences() {
    final List<ChatUser> audience = new ArrayList<>();

    for (final Player player : Bukkit.getOnlinePlayers()) {
      final ChatUser playerUser = this.carbonChat.userService().wrap(player);

      if (this.canPlayerSee(playerUser, true)) {
        audience.add(playerUser);
      }
    }

    return audience;
  }

  private void updateUserNickname(@NonNull final ChatUser user) {
    if (user.online()) {
      String nickname = user.nickname();

      if (nickname != null) {
        final Component component = this.carbonChat.adventureManager().processMessage(nickname);
        nickname = CarbonChat.LEGACY.serialize(component);

        user.player().setDisplayName(nickname);

        if (this.carbonChat.getConfig().getBoolean("nicknames-set-tab-name")) {
          user.player().setPlayerListName(nickname);
        }
      }
    }
  }

  @Override
  @NonNull
  public Component sendMessage(@NonNull final ChatUser user, @NonNull final Collection<@NonNull ChatUser> recipients, @NonNull final String message, final boolean fromRemote) {
    this.updateUserNickname(user);

    // Get player's formatting
    final String messageFormat = this.format(user);

    // Call custom chat event
    final PreChatFormatEvent preFormatEvent = new PreChatFormatEvent(user, this, messageFormat, message);

    CarbonEvents.post(preFormatEvent);

    // Return if cancelled or message is emptied

    if (preFormatEvent.cancelled() || preFormatEvent.message().trim().isEmpty()) {
      return TextComponent.empty();
    }

    final String displayName;

    if (user.nickname() != null) {
      displayName = user.nickname();
    } else {
      if (user.online()) {
        displayName = user.player().getDisplayName();
      } else {
        displayName = user.offlinePlayer().getName();
      }
    }

    // Iterate through players who should receive messages in this channel
    for (final ChatUser target : recipients) {
      // Call second format event. Used for relational stuff (placeholders etc)
      final ChatFormatEvent formatEvent = new ChatFormatEvent(user, target, this, preFormatEvent.format(), preFormatEvent.message());

      CarbonEvents.post(formatEvent);

      // Again, return if cancelled or message is emptied
      if (formatEvent.cancelled() || formatEvent.message().trim().isEmpty()) {
        continue;
      }

      final TextColor targetColor = this.channelColor(target);

      TextComponent formatComponent = (TextComponent) this.carbonChat.adventureManager().processMessage(formatEvent.format(),
        "br", "\n",
        "displayname", displayName,
        "color", "<" + targetColor.asHexString() + ">",
        "phase", Long.toString(System.currentTimeMillis() % 25),
        "server", this.carbonChat.getConfig().getString("server-name", "Server"),
        "message", formatEvent.message());

      if (this.isUserSpying(user, target)) {
        final String prefix = this.processPlaceholders(user, this.carbonChat.getConfig().getString("spy-prefix"));

        formatComponent = (TextComponent) MiniMessage.get().parse(prefix, "color",
          targetColor.asHexString()).append(formatComponent);
      }

      final ChatComponentEvent newEvent = new ChatComponentEvent(user, target, this, formatComponent,
        formatEvent.message());

      CarbonEvents.post(newEvent);

      target.sendMessage(newEvent.component());
    }

    final ChatFormatEvent consoleFormatEvent = new ChatFormatEvent(user, null, this, preFormatEvent.format(),
      preFormatEvent.message());

    CarbonEvents.post(consoleFormatEvent);

    final TextColor targetColor = this.channelColor(user);

    final TextComponent consoleFormat = (TextComponent) this.carbonChat.adventureManager().processMessage(consoleFormatEvent.format(),
      "br", "\n",
      "displayname", displayName,
      "color", "<" + targetColor.asHexString() + ">",
      "phase", Long.toString(System.currentTimeMillis() % 25),
      "server", this.carbonChat.getConfig().getString("server-name", "Server"),
      "message", consoleFormatEvent.message());

    final ChatComponentEvent consoleEvent = new ChatComponentEvent(user, null, this, consoleFormat,
      consoleFormatEvent.message());

    CarbonEvents.post(consoleEvent);

    // Route message to bungee / discord (if message originates from this server)
    // Use instanceof and not isOnline, if this message originates from another then the instanceof will
    // fail, but isOnline may succeed if the player is online on both servers (somehow).
    if (user.online() && !fromRemote && (this.bungee() || this.crossServer())) {
      this.sendMessageToBungee(user.player(), consoleEvent.component());
    }

    return consoleEvent.component();
  }

  @Override
  @NonNull
  public Component sendMessage(@NonNull final ChatUser user, @NonNull final String message, final boolean fromRemote) {
    return this.sendMessage(user, this.audiences(), message, fromRemote);
  }

  @Nullable
  public String format(@NonNull final ChatUser user) {
    for (final String group : this.groupOverrides()) {
      if (this.userHasGroup(user, group)) {
        final String format = this.format(group);

        if (format != null) {
          return format;
        }
      }
    }

    if (this.primaryGroupOnly()) {
      return this.primaryGroupFormat(user);
    } else {
      return this.firstFoundUserFormat(user);
    }
  }

  private boolean userHasGroup(@NonNull final ChatUser user, @NonNull final String group) {
    if (user.online()) {
      if (this.carbonChat.permission().playerInGroup(user.player(), group)) {
        return true;
      }
    } else {
      if (this.carbonChat.permission().playerInGroup(null, user.offlinePlayer(), group)) {
        return true;
      }
    }

    if (user.online() && this.permissionGroupMatching()) {
      return user.player().hasPermission("carbonchat.group." + group);
    }

    return false;
  }

  @Nullable
  private String firstFoundUserFormat(@NonNull final ChatUser user) {
    final String[] playerGroups;

    if (user.online()) {
      playerGroups = this.carbonChat.permission().getPlayerGroups(user.player());
    } else {
      playerGroups = this.carbonChat.permission().getPlayerGroups(null, user.offlinePlayer());
    }

    for (final String group : playerGroups) {
      final String groupFormat = this.format(group);

      if (groupFormat != null) {
        return groupFormat;
      }
    }

    return this.defaultFormat();
  }

  @Nullable
  private String primaryGroupFormat(@NonNull final ChatUser user) {
    final String primaryGroup;

    if (user.online()) {
      primaryGroup = this.carbonChat.permission().getPrimaryGroup(user.player());
    } else {
      primaryGroup = this.carbonChat.permission().getPrimaryGroup(null, user.offlinePlayer());
    }

    final String primaryGroupFormat = this.format(primaryGroup);

    if (primaryGroupFormat != null) {
      return primaryGroupFormat;
    }

    return this.defaultFormat();
  }

  @Nullable
  private String defaultFormat() {
    return this.format(this.defaultFormatName());
  }

  @Override
  @Nullable
  public String format(@NonNull final String group) {
    return this.getString("formats." + group);
  }

  private boolean isUserSpying(@NonNull final ChatUser sender, @NonNull final ChatUser target) {
    if (!this.canPlayerSee(sender, target, false)) {
      return target.channelSettings(this).spying();
    }

    return false;
  }

  @Override
  public void sendComponent(@NonNull final ChatUser player, @NonNull final Component component) {
    for (final ChatUser user : this.audiences()) {
      if (!user.ignoringUser(player)) {
        user.sendMessage(component);
      }
    }
  }

  public void sendMessageToBungee(@NonNull final Player player, @NonNull final Component component) {
    this.carbonChat.messageManager().sendMessage("channel-component", player.getUniqueId(), byteArray -> {
      byteArray.writeUTF(this.key());
      byteArray.writeUTF(this.carbonChat.adventureManager().audiences().gsonSerializer().serialize(component));
    });
  }

  @Override
  public boolean canPlayerSee(@NonNull final ChatUser target, final boolean checkSpying) {
    final Player targetPlayer = target.player();

    if (checkSpying && targetPlayer.hasPermission("carbonchat.spy." + this.name())) {
      if (target.channelSettings(this).spying()) {
        return true;
      }
    }

    if (!targetPlayer.hasPermission("carbonchat.channels." + this.name() + ".see")) {
      return false;
    }

    if (this.ignorable()) {
      return !target.channelSettings(this).ignored();
    }

    return true;
  }

  public boolean canPlayerSee(@NonNull final ChatUser sender, @NonNull final ChatUser target, final boolean checkSpying) {
    final Player targetPlayer = target.player();

    if (!this.canPlayerSee(target, checkSpying)) {
      return false;
    }

    if (this.ignorable()) {
      return !target.ignoringUser(sender) || targetPlayer.hasPermission("carbonchat.ignore.exempt");
    }

    return true;
  }

  @Override
  @Nullable
  public TextColor channelColor(@NonNull final ChatUser user) {
    final TextColor userColor = user.channelSettings(this).color();

    if (userColor != null) {
      System.out.println("user color found!");
      return userColor;
    }

    final String input = this.getString("color");

    final TextColor color = CarbonUtils.parseColor(user, input);

    if (color == null && this.carbonChat.getConfig().getBoolean("show-tips")) {
      this.carbonChat.getLogger().warning("Tip: Channel color found (" + color + ") is invalid!");
      this.carbonChat.getLogger().warning("Falling back to #FFFFFF");

      return NamedTextColor.WHITE;
    }

    return color;
  }

  @Nullable
  private String defaultFormatName() {
    return this.getString("default-group");
  }

  @Override
  public boolean isDefault() {
    if (this.config != null && this.config.contains("default")) {
      return this.config.getBoolean("default");
    }

    return false;
  }

  @Override
  public boolean ignorable() {
    return this.getBoolean("ignorable");
  }

  @Override
  @Deprecated
  public boolean bungee() {
    return this.getBoolean("should-bungee");
  }

  @Override
  public boolean crossServer() {
    return this.getBoolean("is-cross-server");
  }

  @Override
  public boolean honorsRecipientList() {
    return this.getBoolean("honors-recipient-list");
  }

  @Override
  public boolean permissionGroupMatching() {
    return this.getBoolean("permission-group-matching");
  }

  @Override
  @NonNull
  public List<@NonNull String> groupOverrides() {
    return this.getStringList("group-overrides");
  }

  @Override
  @NonNull
  public String name() {
    final String name = this.getString("name");
    return name == null ? this.key : name;
  }

  @Override
  @Nullable
  public String messagePrefix() {
    if (this.config != null && this.config.contains("message-prefix")) {
      return this.config.getString("message-prefix");
    }

    return null;
  }

  @Override
  @Nullable
  public String switchMessage() {
    return this.getString("switch-message");
  }

  @Override
  @Nullable
  public String switchOtherMessage() {
    return this.getString("switch-other-message");
  }

  @Override
  @Nullable
  public String switchFailureMessage() {
    return this.getString("switch-failure-message");
  }

  @Override
  @Nullable
  public String cannotIgnoreMessage() {
    return this.getString("cannot-ignore-message");
  }

  @Override
  @Nullable
  public String toggleOffMessage() {
    return this.getString("toggle-off-message");
  }

  @Override
  @Nullable
  public String toggleOnMessage() {
    return this.getString("toggle-on-message");
  }

  @Override
  @Nullable
  public String toggleOtherOnMessage() {
    return this.getString("toggle-other-on");
  }

  @Override
  @Nullable
  public String toggleOtherOffMessage() {
    return this.getString("toggle-other-off");
  }

  @Override
  @Nullable
  public String cannotUseMessage() {
    return this.getString("cannot-use-channel");
  }

  @Override
  public boolean primaryGroupOnly() {
    return this.getBoolean("primary-group-only");
  }

  @Override
  public boolean shouldCancelChatEvent() {
    return this.getBoolean("cancel-message-event");
  }

  @Override
  @NonNull
  public List<@NonNull Pattern> itemLinkPatterns() {
    final ArrayList<Pattern> itemPatterns = new ArrayList<>();

    for (final String entry : this.carbonChat.getConfig().getStringList("item-link-placeholders")) {
      itemPatterns.add(Pattern.compile(Pattern.quote(entry)));
    }

    return itemPatterns;
  }

  @Override
  @Nullable
  public Object context(@NonNull final String key) {
    final ConfigurationSection section = this.config == null ? null : this.config.getConfigurationSection("contexts");

    if (section == null) {
      final ConfigurationSection defaultSection = this.carbonChat.getConfig().getConfigurationSection("default");

      if (defaultSection == null) {
        return null;
      }

      final ConfigurationSection defaultContexts = defaultSection.getConfigurationSection("contexts");

      if (defaultContexts == null) {
        return null;
      }

      return defaultContexts.get(key);
    }

    return section.get(key);
  }

  @Nullable
  @SuppressWarnings("checkstyle:MethodName")
  private String getString(@NonNull final String key) {
    if (this.config != null && this.config.contains(key)) {
      return this.config.getString(key);
    }

    final ConfigurationSection defaultSection = this.carbonChat.getConfig().getConfigurationSection("default");

    if (defaultSection != null && defaultSection.contains(key)) {
      return defaultSection.getString(key);
    }

    return null;
  }

  @NonNull
  @SuppressWarnings("checkstyle:MethodName")
  private List<@NonNull String> getStringList(@NonNull final String key) {
    if (this.config != null && this.config.contains(key)) {
      return this.config.getStringList(key);
    }

    final ConfigurationSection defaultSection = this.carbonChat.getConfig().getConfigurationSection("default");

    if (defaultSection != null && defaultSection.contains(key)) {
      return defaultSection.getStringList(key);
    }

    return Collections.emptyList();
  }

  @SuppressWarnings("checkstyle:MethodName")
  private boolean getBoolean(@NonNull final String key) {
    if (this.config != null && this.config.contains(key)) {
      return this.config.getBoolean(key);
    }

    final ConfigurationSection defaultSection = this.carbonChat.getConfig().getConfigurationSection("default");

    if (defaultSection != null && defaultSection.contains(key)) {
      return defaultSection.getBoolean(key);
    }

    return false;
  }

  @SuppressWarnings("checkstyle:MethodName")
  private double getDouble(@NonNull final String key) {
    if (this.config != null && this.config.contains(key)) {
      return this.config.getDouble(key);
    }

    final ConfigurationSection defaultSection = this.carbonChat.getConfig().getConfigurationSection("default");

    if (defaultSection != null && defaultSection.contains(key)) {
      return defaultSection.getDouble(key);
    }

    return 0;
  }

  @Override
  @NonNull
  public String key() {
    return this.key;
  }

  @Override
  @Nullable
  public String aliases() {
    final String aliases = this.getString("aliases");

    if (aliases == null) {
      return this.key();
    }

    return aliases;
  }
}
