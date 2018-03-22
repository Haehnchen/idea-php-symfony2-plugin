package fr.adrienbrault.idea.symfony2plugin.dic.intention;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.components.JBList;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpServiceArgumentIntention extends PsiElementBaseIntentionAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if(phpClass != null) {
            Set<String> serviceNames = ContainerCollectionResolver.ServiceCollector.create(project).convertClassNameToServices(phpClass.getFQN());
            if(serviceNames.size() > 0) {
                Collection<PsiElement> psiElements = new ArrayList<>();
                for(String serviceName: serviceNames) {
                    psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
                }

                if(psiElements.size() > 0) {
                    Map<String, PsiElement> map = new HashMap<>();

                    for (PsiElement element : psiElements) {
                        map.put(VfsUtil.getRelativePath(element.getContainingFile().getVirtualFile(), element.getProject().getBaseDir()), element);
                    }

                    final JBList<String> list = new JBList<>(map.keySet());

                    JBPopupFactory.getInstance().createListPopupBuilder(list)
                        .setTitle("Symfony: Services Definitions")
                        .setItemChoosenCallback(() -> new WriteCommandAction.Simple(editor.getProject(), "Service Update") {
                            @Override
                            protected void run() {
                                String selectedValue = list.getSelectedValue();

                                PsiElement target = map.get(selectedValue);
                                invokeByScope(target, editor);
                            }
                        }.execute())
                        .createPopup()
                        .showInBestPositionFor(editor);
                }
            }
        }
    }

    private void invokeByScope(@NotNull PsiElement psiElement, @NotNull Editor editor) {
        boolean success = false;

        if(psiElement instanceof XmlTag) {
            List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes((XmlTag) psiElement, true, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject()));

            success = args.size() > 0;
            if (success) {
                ServiceActionUtil.fixServiceArgument(args, (XmlTag) psiElement);
            }
        } else if(psiElement instanceof YAMLKeyValue) {
            List<String> args = ServiceActionUtil.getYamlMissingArgumentTypes(
                psiElement.getProject(),
                ServiceActionUtil.ServiceYamlContainer.create((YAMLKeyValue) psiElement),
                false,
                new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
            );

            success = args.size() > 0;
            if (success) {
                ServiceActionUtil.fixServiceArgument((YAMLKeyValue) psiElement);
            }
        }

        if(!success) {
            HintManager.getInstance().showErrorHint(editor, "No argument update needed");
            return;
        }

        String relativePath = VfsUtil.getRelativePath(psiElement.getContainingFile().getVirtualFile(), psiElement.getProject().getBaseDir());
        if(relativePath == null) {
            relativePath = "n/a";
        }

        HintManager.getInstance().showInformationHint(editor, String.format("Argument updated: %s", relativePath));
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return psiElement.getLanguage().equals(PhpLanguage.INSTANCE) && getServicesInScope(psiElement).size() > 0;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Update service arguments";
    }

    @NotNull
    private Set<String> getServicesInScope(@NotNull PsiElement psiElement) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);

        return phpClass == null
            ? Collections.emptySet()
            : ContainerCollectionResolver.ServiceCollector.create(psiElement.getProject()).convertClassNameToServices(phpClass.getFQN());
    }
}
