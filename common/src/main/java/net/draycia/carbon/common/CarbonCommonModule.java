package net.draycia.carbon.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.proximyst.moonshine.Moonshine;
import net.draycia.carbon.common.config.ConfigLoader;
import net.draycia.carbon.common.config.PrimaryConfig;
import net.draycia.carbon.common.messages.CarbonMessageParser;
import net.draycia.carbon.common.messages.CarbonMessageSender;
import net.draycia.carbon.common.messages.CarbonMessageService;
import net.draycia.carbon.common.messages.CarbonMessageSource;
import net.draycia.carbon.common.messages.ComponentPlaceholderResolver;
import net.draycia.carbon.common.messages.ServerReceiverResolver;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class CarbonCommonModule extends AbstractModule {

    @Provides
    @Singleton
    public ConfigLoader configLoader() {
        return new ConfigLoader();
    }

    @Provides
    @Singleton
    public PrimaryConfig primaryConfig(final ConfigLoader configLoader) {
        final @Nullable PrimaryConfig primaryConfig =
            configLoader.load(PrimaryConfig.class, "config.conf");

        if (primaryConfig == null) {
            throw new IllegalStateException("Primary configuration was unable to load!");
        }

        return primaryConfig;
    }

    @Provides
    @Singleton
    public CarbonMessageService messageService(
        final ServerReceiverResolver serverReceiverResolver,
        final ComponentPlaceholderResolver<Audience> componentPlaceholderResolver,
        final CarbonMessageSource carbonMessageSource,
        final CarbonMessageParser carbonMessageParser,
        final CarbonMessageSender carbonMessageSender
    ) {
        return Moonshine.<Audience>builder()
            .receiver(serverReceiverResolver)
            .placeholder(Component.class, componentPlaceholderResolver)
            .source(carbonMessageSource)
            .parser(carbonMessageParser)
            .sender(carbonMessageSender)
            .create(CarbonMessageService.class, this.getClass().getClassLoader());
    }

}
