package net.draycia.carbon.common.messages;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.proximyst.moonshine.message.IMessageSource;
import net.draycia.carbon.api.CarbonChat;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

<<<<<<< HEAD
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
=======
import java.io.*;
>>>>>>> Initial pass - 1/?
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Singleton
@DefaultQualifier(NonNull.class)
public class CarbonMessageSource implements IMessageSource<String, Audience> {

    private final Properties properties;

    @Inject
    CarbonMessageSource(final Path dataDirectory, final CarbonChat carbonChat) throws IOException {
        this.properties = new Properties();

        // TODO: read file name from config, allow users to specify which file
        final var fileName = "messages-en.properties";
        final var file = dataDirectory.resolve(fileName).toFile();

        if (file.isFile()) {
            try (final Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                this.properties.load(reader);
            }
        }

        boolean write = !file.isFile();

        try (final InputStream stream = carbonChat.getClass().getResourceAsStream("/" + fileName)) {
            final Properties packaged = new Properties();
            packaged.load(stream);

            for (final Map.Entry<Object, Object> entry : packaged.entrySet()) {
                write |= this.properties.putIfAbsent(entry.getKey(), entry.getValue()) == null;
            }
        }

        if (write) {
            try (final Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                this.properties.store(writer, null);
            }
        }
    }

    @Override
    public String message(final String key, final Audience receiver) {
        final String value = this.properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("No message mapping for key " + key);
        }

        return value;
    }

}
