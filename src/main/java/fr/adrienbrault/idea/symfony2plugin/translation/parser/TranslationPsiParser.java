package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationPsiParser {
    @NotNull
    private final Project project;

    @NotNull
    private final Collection<File> paths;

    @NotNull final
    private TranslationStringMap translationStringMap;

    public TranslationPsiParser(@NotNull Project project, @NotNull Collection<File> paths) {
        this.project = project;
        this.paths = paths;
        this.translationStringMap = new TranslationStringMap();
    }

    public TranslationStringMap parsePathMatcher() {
        for (File path : paths) {
            File[] files = path.listFiles((directory, s) -> s.startsWith("catalogue") && s.endsWith("php"));
            if(null == files || files.length == 0) {
                continue;
            }

            for (final File fileEntry : files) {
                this.parse(fileEntry);
                this.translationStringMap.addFile(fileEntry.getName(), fileEntry.lastModified());
            }
        }

        return this.translationStringMap;
    }

    public void parse(File file) {
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
        if(virtualFile == null) {
            Symfony2ProjectComponent.getLogger().info("VfsUtil missing translation: " + file.getPath());
            return;
        }
        
        parse(virtualFile);
    }
    
    public void parse(@NotNull VirtualFile virtualFile) {
        PsiFile psiFile;
        try {
            psiFile = PhpPsiElementFactory.createPsiFileFromText(this.project, StreamUtil.readText(virtualFile.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            return;
        }

        if(psiFile == null) {
            return;
        }

        Symfony2ProjectComponent.getLogger().info("update translations: " + virtualFile.getPath());

        Collection<NewExpression> messageCatalogues = PsiTreeUtil.collectElementsOfType(psiFile, NewExpression.class);
        for (NewExpression newExpression: messageCatalogues) {
            boolean isMessageCatalogueConstructor = false;
            for (PhpClass phpClass : PhpElementsUtil.getNewExpressionPhpClasses(newExpression)) {
                if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Translation\\MessageCatalogue") || PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Translation\\MessageCatalogueInterface")) {
                    isMessageCatalogueConstructor = true;
                    break;
                }
            }

            if(isMessageCatalogueConstructor) {
                this.getTranslationMessages(newExpression);
            }
        }
    }

    private void getTranslationMessages(NewExpression newExpression) {
        // first parameter hold our huge translation arrays
        PsiElement[] parameters = newExpression.getParameters();
        if(parameters.length < 2 || !(parameters[1] instanceof ArrayCreationExpression)) {
            return;
        }

        Collection<ArrayHashElement> domainArrays = PsiTreeUtil.getChildrenOfTypeAsList(parameters[1], ArrayHashElement.class);
        for(ArrayHashElement arrayHashElement: domainArrays) {
            PhpPsiElement arrayKey = arrayHashElement.getKey();
            if(arrayKey instanceof StringLiteralExpression) {
                String transDomain = ((StringLiteralExpression) arrayKey).getContents();
                this.translationStringMap.addDomain(transDomain);

                // parse translation keys
                PhpPsiElement arrayValue = arrayHashElement.getValue();
                if(arrayValue instanceof ArrayCreationExpression) {
                    getTransKeys(transDomain, (ArrayCreationExpression) arrayValue);
                }
            }
        }
    }

    private void getTransKeys(String domain, ArrayCreationExpression translationArray) {
        Collection<ArrayHashElement> test = PsiTreeUtil.getChildrenOfTypeAsList(translationArray, ArrayHashElement.class);
        for(ArrayHashElement arrayHashElement: test) {
            PhpPsiElement translationKey = arrayHashElement.getKey();
            if(translationKey instanceof StringLiteralExpression) {
                String transKey = ((StringLiteralExpression) translationKey).getContents();
                this.translationStringMap.addString(domain, transKey);
            }
        }
    }
}
