package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MailCollectorInterface {
    @NotNull
    Collection<MailMessage> getMessages();
}
