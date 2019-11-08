package fr.adrienbrault.idea.symfonyplugin.profiler.collector;

import fr.adrienbrault.idea.symfonyplugin.profiler.dict.MailMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MailCollectorInterface {
    @NotNull
    Collection<MailMessage> getMessages();
}
