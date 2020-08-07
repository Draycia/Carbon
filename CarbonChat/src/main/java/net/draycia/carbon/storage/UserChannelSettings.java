package net.draycia.carbon.storage;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public interface UserChannelSettings {

    boolean isSpying();
    void setSpying(boolean spying, boolean fromRemote);
    default void setSpying(boolean spying) {
        this.setSpying(spying, false);
    }

    boolean isIgnored();
    void setIgnoring(boolean ignored, boolean fromRemote);
    default void setIgnoring(boolean ignored) {
        this.setIgnoring(ignored, false);
    }

    @Nullable TextColor getColor();
    void setColor(@Nullable TextColor color, boolean fromRemote);
    default void setColor(@Nullable TextColor color) {
        this.setColor(color, false);
    }

}
