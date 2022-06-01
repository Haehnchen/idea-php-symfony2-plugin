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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationStringMap {
    @NotNull
    private final Map<String, Set<String>> domainMap;

    private TranslationStringMap() {
        this.domainMap = new ConcurrentHashMap<>();
    }

    @Nullable
    public Set<String> getDomainMap(@NotNull String domainKey) {
        if(!domainMap.containsKey(domainKey)) {
            return null;
        }

        return domainMap.get(domainKey);
    }

    private void addString(@NotNull String domain, @NotNull String stringId) {
        domainMap.putIfAbsent(domain, new HashSet<>());
        domainMap.get(domain).add(stringId);
    }

    @NotNull
    public Set<String> getDomainList() {
        return domainMap.keySet();
    }

    private void addDomain(@NotNull String domain) {
        domainMap.putIfAbsent(domain, new HashSet<>());
    }

    @NotNull
    public static TranslationStringMap createEmpty() {
        return new TranslationStringMap();
    }

    @NotNull
    public static TranslationStringMap create(@NotNull Project project, @NotNull Collection<File> paths) {
       TranslationStringMap translationStringMap = new TranslationStringMap();

        for (File path : paths) {
            File[] files = path.listFiles((directory, s) -> s.startsWith("catalogue") && s.endsWith("php"));
            if(null == files || files.length == 0) {
                continue;
            }

            for (final File fileEntry : files) {
                translationStringMap.parse(project, fileEntry);
            }
        }

        return translationStringMap;
    }

    private void parse(@NotNull Project project, @NotNull File file) {
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
        if (virtualFile == null) {
            Symfony2ProjectComponent.getLogger().info("VfsUtil missing translation: " + file.getPath());
            return;
        }

        parse(project, virtualFile);
    }

    public void parse(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        PsiFile psiFile;
        try {
            psiFile = PhpPsiElementFactory.createPsiFileFromText(project, StreamUtil.readText(virtualFile.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            return;
        }

        if (psiFile == null) {
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

            if (isMessageCatalogueConstructor) {
                this.getTranslationMessages(newExpression);
            }
        }
    }

    private void getTranslationMessages(@NotNull NewExpression newExpression) {
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
                if (StringUtils.isBlank(transDomain)) {
                    continue;
                }

                if (transDomain.endsWith("+intl-icu")) {
                    transDomain = transDomain.substring(0, transDomain.length() - 9);
                }

                if (StringUtils.isBlank(transDomain)) {
                    continue;
                }

                addDomain(transDomain);

                // parse translation keys
                PhpPsiElement arrayValue = arrayHashElement.getValue();
                if(arrayValue instanceof ArrayCreationExpression) {
                    getTransKeys(transDomain, (ArrayCreationExpression) arrayValue);
                }
            }
        }
    }

    private void getTransKeys(@NotNull String domain, @NotNull ArrayCreationExpression translationArray) {
        Collection<ArrayHashElement> test = PsiTreeUtil.getChildrenOfTypeAsList(translationArray, ArrayHashElement.class);
        for (ArrayHashElement arrayHashElement: test) {
            PhpPsiElement translationKey = arrayHashElement.getKey();
            if (translationKey instanceof StringLiteralExpression) {
                String transKey = ((StringLiteralExpression) translationKey).getContents();
                addString(domain, transKey);
            }
        }
    }
}

