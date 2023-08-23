package fr.adrienbrault.idea.symfony2plugin.tests.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalMailCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalMailCollectorTest extends TestCase {
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/profiler/collector/fixtures";
    }

    public void testFoo() {
        String data = ProfilerUtil.getContentForFile(new File(this.getTestDataPath() + "/mailer-dc7bb5"));

        LocalMailCollector localMailCollector = new LocalMailCollector(data);

        Collection<MailMessage> messages = localMailCollector.getMessages();

        assertTrue(messages.stream().anyMatch(mailMessage -> "Time for Symfony Mailer!".equals(mailMessage.title())));
    }
}
