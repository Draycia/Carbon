package net.draycia.carbon.common.users;

import java.util.Locale;
import java.util.UUID;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static java.util.Objects.requireNonNullElseGet;
import static net.kyori.adventure.text.Component.text;

@DefaultQualifier(NonNull.class)
public abstract class CarbonPlayerCommon implements CarbonPlayer, ForwardingAudience.Single {

    protected @Nullable Component displayName;
    protected Identity identity;
    protected @Nullable ChatChannel selectedChannel;
    protected String username;
    protected UUID uuid;

    protected CarbonPlayerCommon(
        final Identity identity,
        final String username,
        final UUID uuid
    ) {
        this.displayName = text(username);
        this.identity = identity;
        this.selectedChannel = null;
        this.username = username;
        this.uuid = uuid;
    }

    protected CarbonPlayerCommon() {

    }

    @Override
    public @NonNull Audience audience() {
        return Audience.empty();
    }

    abstract protected CarbonPlayer carbonPlayer();

    @Override
    public Component createItemHoverComponent() {
        return Component.empty();
    }

    @Override
    public Component displayName() {
        return requireNonNullElseGet(this.displayName, () -> text(this.username));
    }

    @Override
    public void displayName(final @Nullable Component displayName) {
        this.displayName = requireNonNullElseGet(displayName, () -> text(this.username));
    }

    @Override
    public boolean hasPermission(String permission) {
        return false;
    }

    @Override
    public Identity identity() {
        return this.identity;
    }

    @Override
    public @Nullable Locale locale() {
        return Locale.getDefault();
    }

    @Override
    public @Nullable ChatChannel selectedChannel() {
        return this.selectedChannel;
    }

    @Override
    public void selectedChannel(final ChatChannel chatChannel) {
        this.selectedChannel = chatChannel;
    }

    @Override
    public String username() {
        return this.username;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }
}
