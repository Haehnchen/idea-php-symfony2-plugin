package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleColoredComponent;
import com.jetbrains.php.PhpIcons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.DefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequest;
import icons.TwigIcons;
import org.jdesktop.swingx.renderer.IconValue;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

public class Symfony2WebProfilerForm {
    private JPanel panel1;
    private JTabbedPane tabbedPane1;
    private JEditorPane editorPane1;
    private JList list1;
    private JButton button1;
    private JButton button2;
    private JList listRequest;
    private JList listRequestDetails;

    public DefaultListModel listenModel = new DefaultListModel();
    public DefaultListModel modelRequests = new DefaultListModel();
    public DefaultListModel modelRequestsDetails = new DefaultListModel();

    private Project project;
    private ProfilerIndex profilerIndex;

    public Symfony2WebProfilerForm(Project project) {
        this.project = project;

        this.list1.setModel(this.listenModel);
        this.list1.setCellRenderer(new MyLookupCellRenderer());


        this.listRequest.setModel(this.modelRequests);
        this.listRequest.setCellRenderer(new RequestCellRender());

        this.listRequestDetails.setModel(this.modelRequestsDetails);
        this.listRequestDetails.setCellRenderer(new RequestDetailsCellRender());

        this.profilerIndex = new ProfilerIndex(this.getTranslationFile());

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
                    if (list.getSelectedValue() instanceof ProfilerRequest) {
                        Symfony2WebProfilerForm.this.selected((ProfilerRequest) list.getSelectedValue());
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
                    if (list.getSelectedValue() instanceof ProfilerRequest) {
                        Symfony2WebProfilerForm.this.renderRequestDetails((ProfilerRequest) list.getSelectedValue());
                    }
                }
            }
        });

    }

    public void renderRequests() {
        DefaultListModel listModel = (DefaultListModel) listRequest.getModel();
        listModel.removeAllElements();

        for(ProfilerRequest profilerRequest: this.profilerIndex.getRequests()) {
            listModel.addElement(profilerRequest);
        }

    }


    public void renderRequestDetails(ProfilerRequest profilerRequest) {
        DefaultListModel listModel = (DefaultListModel) listRequestDetails.getModel();
        listModel.removeAllElements();

        DefaultDataCollector defaultDataCollector = profilerRequest.getCollector(DefaultDataCollector.class);

        listModel.addElement(new RequestDetails(defaultDataCollector.getStatusCode(), IconValue.NULL_ICON));
        listModel.addElement(new RequestDetails(defaultDataCollector.getRoute(), Symfony2Icons.ROUTE));
        listModel.addElement(new RequestDetails(defaultDataCollector.getController(), PhpIcons.METHOD_ICON));
        listModel.addElement(new RequestDetails(defaultDataCollector.getTemplate(), TwigIcons.TwigFileIcon));

    }

    public JComponent createComponent() {
        return panel1;
    }

    public void selected(ProfilerRequest profilerRequest) {
        ArrayList<MailMessage> messages = profilerRequest.getCollector(MailCollector.class).getMessages();
        if(messages.size() > 0) {
            this.editorPane1.setText(messages.get(0).getMessage());
        }
    }

    public void selected(MailMessage mailMessage) {
        this.editorPane1.setText(mailMessage.getMessage());
    }


    public void start() {

        DefaultListModel listModel = (DefaultListModel) list1.getModel();
        listModel.removeAllElements();

        for(ProfilerRequest profilerRequest: this.profilerIndex.getRequests()) {
            ArrayList<MailMessage> messages = profilerRequest.getCollector(MailCollector.class).getMessages();
            if(messages.size() > 0) {
                for(MailMessage message : messages) {
                    listModel.addElement(message);
                }
            }
        }

    }


    @Nullable
    protected File getTranslationFile() {

        Symfony2ProjectComponent symfony2ProjectComponent = this.project.getComponent(Symfony2ProjectComponent.class);
        for(File file: symfony2ProjectComponent.getContainerFiles()) {
            if(file.exists()) {
                File translationRootPath = new File(file.getParentFile().getPath() + "/profiler/index.csv");
                if (!translationRootPath.exists()) {
                    return translationRootPath;
                }
            }
        }

        return null;
    }

    private class MyLookupCellRenderer extends SimpleColoredComponent implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if(value instanceof ProfilerRequest) {
                renderer.setText(((ProfilerRequest) value).getUrl());
            }

            if(value instanceof MailMessage) {
                renderer.setText(((MailMessage) value).getTitle());
            }

            return renderer;
        }
    }


    private class RequestCellRender extends SimpleColoredComponent implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if(value instanceof ProfilerRequest) {
                renderer.setText(((ProfilerRequest) value).getUrl());
            }

            return renderer;
        }
    }

    private class RequestDetailsCellRender extends SimpleColoredComponent implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

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

    private class RequestDetails {

        private String text;
        private Icon icon;

        public RequestDetails(String text, Icon icon) {
            this.text = text;
            this.icon = icon;
        }

        private String getText() {
            return text;
        }

        private Icon getIcon() {
            return icon;
        }

    }

}
