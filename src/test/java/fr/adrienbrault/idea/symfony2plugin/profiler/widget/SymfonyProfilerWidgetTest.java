package fr.adrienbrault.idea.symfony2plugin.profiler.widget;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.MailCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SymfonyProfilerWidgetTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testBuildActionsParallelDoesNotCreateNullActions() {
        List<ProfilerRequestInterface> requests = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            requests.add(new TestProfilerRequest(
                "hash-" + i,
                "http://example.com/req-" + i,
                new TestDefaultCollector("route-" + (i % 5), "template-" + (i % 3), "Controller::method" + i, "FormType" + (i % 7)),
                new TestMailCollector(new MailMessage("body", "Mail " + i, "html", "swiftmailer"))
            ));
        }

        ProfilerIndexInterface index = new ProfilerIndexInterface() {
            @NotNull
            @Override
            public List<ProfilerRequestInterface> getRequests() {
                return requests;
            }

            @Nullable
            @Override
            public String getUrlForRequest(@NotNull ProfilerRequestInterface request) {
                return "http://example.com/_profiler/" + request.getHash();
            }
        };

        DefaultActionGroup actionGroup = SymfonyProfilerWidget.buildActions(getProject(), index, requests);
        AnAction[] actions = actionGroup.getChildren((com.intellij.openapi.actionSystem.AnActionEvent) null);

        assertTrue(actions.length > 0);
        for (AnAction action : actions) {
            assertNotNull(action);
        }
    }

    private static class TestProfilerRequest implements ProfilerRequestInterface {
        private final String hash;
        private final String url;
        private final DefaultDataCollectorInterface defaultCollector;
        private final MailCollectorInterface mailCollector;

        private TestProfilerRequest(String hash, String url, DefaultDataCollectorInterface defaultCollector, MailCollectorInterface mailCollector) {
            this.hash = hash;
            this.url = url;
            this.defaultCollector = defaultCollector;
            this.mailCollector = mailCollector;
        }

        @NotNull
        @Override
        public String getHash() {
            return hash;
        }

        @Nullable
        @Override
        public String getMethod() {
            return "GET";
        }

        @NotNull
        @Override
        public String getUrl() {
            return url;
        }

        @NotNull
        @Override
        public String getProfilerUrl() {
            return "http://example.com/_profiler/" + hash;
        }

        @Override
        public int getStatusCode() {
            return 200;
        }

        @Nullable
        @Override
        public <T> T getCollector(Class<T> classFactory) {
            if (classFactory == DefaultDataCollectorInterface.class) {
                return classFactory.cast(defaultCollector);
            }

            if (classFactory == MailCollectorInterface.class) {
                return classFactory.cast(mailCollector);
            }

            return null;
        }
    }

    private static class TestDefaultCollector implements DefaultDataCollectorInterface {
        private final String route;
        private final String template;
        private final String controller;
        private final String formType;

        private TestDefaultCollector(String route, String template, String controller, String formType) {
            this.route = route;
            this.template = template;
            this.controller = controller;
            this.formType = formType;
        }

        @Nullable
        @Override
        public String getController() {
            return controller;
        }

        @Nullable
        @Override
        public String getRoute() {
            return route;
        }

        @Nullable
        @Override
        public String getTemplate() {
            return template;
        }

        @NotNull
        @Override
        public Collection<String> getFormTypes() {
            return Collections.singletonList(formType);
        }
    }

    private static class TestMailCollector implements MailCollectorInterface {
        private final Collection<MailMessage> messages;

        private TestMailCollector(MailMessage message) {
            this.messages = Collections.singletonList(message);
        }

        @NotNull
        @Override
        public Collection<MailMessage> getMessages() {
            return messages;
        }
    }
}
