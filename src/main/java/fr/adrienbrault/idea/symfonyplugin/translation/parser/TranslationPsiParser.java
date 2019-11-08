package fr.adrienbrault.idea.symfonyplugin.translation.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationPsiParser {

    private Project project;
    private TranslationStringMap translationStringMap;

    public TranslationPsiParser(Project project) {
        this.project = project;
        this.translationStringMap = new TranslationStringMap();
    }

    public TranslationStringMap parsePathMatcher(String path) {

        File file = new File(path);
        File[] files = file.listFiles();

        if(null == files) {
            return this.translationStringMap;
        }

        for (final File fileEntry : files) {
            if (!fileEntry.isDirectory()) {
                String fileName = fileEntry.getName();
                if(fileName.startsWith("catalogue") && fileName.endsWith("php")) {
                    this.parse(fileEntry);
                    this.translationStringMap.addFile(fileName, fileEntry.lastModified());
                }
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

        PsiFile psiFile;
        try {
            psiFile = PhpPsiElementFactory.createPsiFileFromText(this.project, StreamUtil.readText(virtualFile.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            return;
        }

        if(psiFile == null) {
            return;
        }

        Symfony2ProjectComponent.getLogger().info("update translations: " + file.getPath());

        Collection<NewExpression> messageCatalogues = PsiTreeUtil.collectElementsOfType(psiFile, NewExpression.class);
        for(NewExpression newExpression: messageCatalogues) {
            ClassReference classReference = newExpression.getClassReference();
            if(classReference != null) {
                PsiElement constructorMethod = classReference.resolve();
                if(constructorMethod instanceof Method) {
                    PhpClass phpClass = ((Method) constructorMethod).getContainingClass();
                    if(phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Translation\\MessageCatalogueInterface")) {
                        this.getTranslationMessages(newExpression);
                    }

                }

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
