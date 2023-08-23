package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleColoredComponent;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.MailCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.LocalProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil;
import icons.TwigIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class Symfony2WebProfilerForm {
    private JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JEditorPane editorPane1;
    private JList list1;
    private JButton button1;
    private JButton button2;
    private JList<ProfilerRequestInterface> listRequest;
    private JList<RequestDetails> listRequestDetails;

    private ProfilerIndexInterface profilerIndex;

    Symfony2WebProfilerForm(@NotNull Project project) {
        DefaultListModel listenModel = new DefaultListModel();
        this.list1.setModel(listenModel);
        this.list1.setCellRenderer(new MyLookupCellRenderer());

        DefaultListModel<ProfilerRequestInterface> modelRequests = new DefaultListModel<>();
        this.listRequest.setModel(modelRequests);
        this.listRequest.setCellRenderer(new RequestCellRender());

        DefaultListModel<RequestDetails> modelRequestsDetails = new DefaultListModel<>();
        this.listRequestDetails.setModel(modelRequestsDetails);
        this.listRequestDetails.setCellRenderer(new RequestDetailsCellRender());

        this.profilerIndex = ProfilerFactoryUtil.createIndex(project);
        if(this.profilerIndex == null) {
            return;
        }

        button1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                Symfony2WebProfilerForm.this.start();
            }
        });

        list1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                JList list = (JList) e.getSource();
                if (e.getClickCount() == 2) {
                    if (list.getSelectedValue() instanceof LocalProfilerRequest) {
                        Symfony2WebProfilerForm.this.selected((LocalProfilerRequest) list.getSelectedValue());
                    }
                    if (list.getSelectedValue() instanceof MailMessage) {
                        Symfony2WebProfilerForm.this.selected((MailMessage) list.getSelectedValue());
                    }
                }

            }
        });

        button2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Symfony2WebProfilerForm.this.renderRequests();
            }
        });

        listRequest.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (e.getClickCount() == 1) {
                    JList list = (JList) e.getSource();
                    if (list.getSelectedValue() instanceof LocalProfilerRequest) {
                        Symfony2WebProfilerForm.this.renderRequestDetails((LocalProfilerRequest) list.getSelectedValue());
                    }
                }
            }
        });

    }

    private void renderRequests() {
        DefaultListModel<ProfilerRequestInterface> listModel = (DefaultListModel<ProfilerRequestInterface>) listRequest.getModel();
        listModel.removeAllElements();

        this.profilerIndex.getRequests().forEach(listModel::addElement);
    }

    private void renderRequestDetails(@NotNull ProfilerRequestInterface profilerRequest) {
        DefaultListModel<RequestDetails> listModel = (DefaultListModel<RequestDetails>) listRequestDetails.getModel();
        listModel.removeAllElements();

        DefaultDataCollectorInterface defaultDataCollector = profilerRequest.getCollector(DefaultDataCollectorInterface.class);
        if(defaultDataCollector != null) {
            if(defaultDataCollector.getRoute() != null) {
                listModel.addElement(new RequestDetails(defaultDataCollector.getRoute(), Symfony2Icons.ROUTE));
            }

            if(defaultDataCollector.getController() != null) {
                listModel.addElement(new RequestDetails(defaultDataCollector.getController(), PhpIcons.METHOD));
            }

            if(defaultDataCollector.getTemplate() != null) {
                listModel.addElement(new RequestDetails(defaultDataCollector.getTemplate(), TwigIcons.TwigFileIcon));
            }
        }
    }

    JComponent createComponent() {
        return panel1;
    }

    public void selected(@NotNull ProfilerRequestInterface profilerRequest) {
        MailCollectorInterface collector = profilerRequest.getCollector(MailCollectorInterface.class);
        if(collector != null) {
            Collection<MailMessage> messages = collector.getMessages();
            if(messages.size() > 0) {
                this.editorPane1.setText(messages.iterator().next().message());
            }
        }
    }

    public void selected(MailMessage mailMessage) {
        this.editorPane1.setText(mailMessage.message());
    }

    private void start() {
        DefaultListModel<MailMessage> listModel = (DefaultListModel) list1.getModel();
        listModel.removeAllElements();

        for(ProfilerRequestInterface profilerRequest: this.profilerIndex.getRequests()) {
            MailCollectorInterface collector = profilerRequest.getCollector(MailCollectorInterface.class);
            if(collector == null) {
                continue;
            }

            Collection<MailMessage> messages = collector.getMessages();

            if(messages.size() > 0) {
                for(MailMessage message : messages) {
                    listModel.addElement(message);
                }
            }
        }
    }

    private class MyLookupCellRenderer extends SimpleColoredComponent implements ListCellRenderer {
        DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(value instanceof LocalProfilerRequest) {
                renderer.setText(((LocalProfilerRequest) value).getUrl());
            }

            if (value instanceof MailMessage) {
                renderer.setText(StringUtils.abbreviate(((MailMessage) value).title(), 40));
            }

            return renderer;
        }
    }

    private class RequestCellRender extends SimpleColoredComponent implements ListCellRenderer {
        DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(value instanceof LocalProfilerRequest) {
                renderer.setText(((LocalProfilerRequest) value).getUrl());
            }

            return renderer;
        }
    }

    private class RequestDetailsCellRender extends SimpleColoredComponent implements ListCellRenderer {
        DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(value instanceof RequestDetails) {
                renderer.setText(((RequestDetails) value).getText());
                renderer.setIcon(((RequestDetails) value).getIcon());
            }

            return renderer;
        }
    }

    private static class RequestDetails {
        @NotNull
        private String text;

        @NotNull
        private Icon icon;

        RequestDetails(@NotNull String text, @NotNull Icon icon) {
            this.text = text;
            this.icon = icon;
        }

        @NotNull
        private String getText() {
            return text;
        }

        @NotNull
        private Icon getIcon() {
            return icon;
        }
    }
}
