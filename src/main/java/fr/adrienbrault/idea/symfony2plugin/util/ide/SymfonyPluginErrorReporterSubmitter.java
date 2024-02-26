package fr.adrienbrault.idea.symfony2plugin.util.ide;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyPluginErrorReporterSubmitter extends ErrorReportSubmitter {
    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        DataContext context = DataManager.getInstance().getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);

        new Task.Backgroundable(project, "Sending Error Report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                JsonObject jsonObject = new JsonObject();

                PluginDescriptor pluginDescriptor = getPluginDescriptor();

                String pluginId = pluginDescriptor.getPluginId().toString();
                String pluginVersion = pluginDescriptor.getVersion();

                jsonObject.addProperty("plugin_id", pluginId);
                jsonObject.addProperty("plugin_version", pluginVersion);

                if (StringUtils.isNotBlank(additionalInfo)) {
                    jsonObject.addProperty("comment", additionalInfo);
                }

                JsonObject ide = new JsonObject();
                ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
                ide.addProperty("version", applicationInfo.getBuild().withoutProductCode().asString());
                ide.addProperty("full_version", applicationInfo.getFullVersion());
                ide.addProperty("build", applicationInfo.getBuild().toString());
                jsonObject.add("ide", ide);

                JsonArray jsonElements = new JsonArray();

                for (IdeaLoggingEvent event : events) {
                    JsonObject jsonEvent = new JsonObject();
                    jsonEvent.addProperty("message", event.getMessage());
                    jsonEvent.addProperty("stacktrace", event.getThrowableText());

                    Throwable throwable = event.getThrowable();
                    if (throwable != null) {
                        jsonEvent.addProperty("stacktrace_message", throwable.getMessage());
                    }

                    jsonElements.add(jsonEvent);
                }

                if (!jsonElements.isEmpty()) {
                    jsonObject.add("events", jsonElements);
                }

                String s = jsonObject.toString();

                ApplicationManager.getApplication().invokeLater(() -> {
                    CloseableHttpClient httpClient = HttpClientBuilder.create().build();


                    boolean success = false;
                    try {
                        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>() {{
                            add(new BasicNameValuePair("plugin", pluginId));
                        }};

                        HttpPost request = new HttpPost("https://espend.de/report-submitter?" + URLEncodedUtils.format(nameValuePairs, "utf-8"));
                        request.addHeader("content-type", "application/json");
                        request.addHeader("x-plugin-version", pluginVersion);

                        request.setEntity(new StringEntity(s));
                        CloseableHttpResponse execute = httpClient.execute(request);
                        httpClient.close();

                        int statusCode = execute.getStatusLine().getStatusCode();
                        success = statusCode >= 200 && statusCode < 300;
                    } catch (Exception ignored) {
                    }

                    if (!success) {
                        return;
                    }

                    consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
                });
            }
        }.queue();

        return true;
    }

    @NotNull
    @Override
    public String getReportActionText() {
        return "Report to espend.de";
    }
}