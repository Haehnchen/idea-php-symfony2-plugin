package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.DispatcherEvent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.EventDispatcherUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventAnnotationStubIndex extends FileBasedIndexExtension<String, DispatcherEvent> {

    public static final ID<String, DispatcherEvent> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.events_annotation");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final ObjectStreamDataExternalizer<DispatcherEvent> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, DispatcherEvent> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, DispatcherEvent, FileContent> getIndexer() {
        return inputData -> {
            Map<String, DispatcherEvent> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            for (PhpNamedElement topLevelElement : ((PhpFile) psiFile).getTopLevelDefs().values()) {
                if (topLevelElement instanceof PhpClass clazz) {
                    for (Field ownField : clazz.getOwnFields()) {
                        PhpDocComment docComment = ownField.getDocComment();
                        if (docComment != null) {
                            PhpDocUtil.processTagElementsByPredicate(
                                docComment,
                                phpDocTag -> visitPhpDocTag(phpDocTag, map),
                                phpDocTag -> true
                            );
                        }
                    }
                }
            }

            return map;
        };

    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<DispatcherEvent> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file ->
            file.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    private void visitPhpDocTag(@NotNull PhpDocTag element, @NotNull Map<String, DispatcherEvent> map) {
        String name = StringUtils.stripStart(element.getName(), "@");
        if(!"Event".equalsIgnoreCase(name)) {
            return;
        }

        PhpDocComment phpDocComment = ObjectUtils.tryCast(element.getParent(), PhpDocComment.class);
        if(phpDocComment == null) {
            return;
        }

        PhpPsiElement nextPsiSibling = phpDocComment.getNextPsiSibling();
        if(nextPsiSibling == null || nextPsiSibling.getNode().getElementType() != PhpElementTypes.CLASS_CONSTANTS) {
            return;
        }

        ClassConstImpl childOfAnyType = PsiTreeUtil.findChildOfAnyType(nextPsiSibling, ClassConstImpl.class);
        if(childOfAnyType == null) {
            return;
        }

        PsiElement defaultValue = childOfAnyType.getDefaultValue();
        if(!(defaultValue instanceof StringLiteralExpression)) {
            return;
        }

        String contents = ((StringLiteralExpression) defaultValue).getContents();

        String fqn = StringUtils.stripStart(childOfAnyType.getFQN(), "\\");

        map.put(contents, new DispatcherEvent(
            fqn,
            findClassInstance(phpDocComment, element))
        );
    }

    private String findClassInstance(@NotNull PhpDocComment phpDocComment, @NotNull PhpDocTag phpDocTag) {
        PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
        if(phpDocAttributeList instanceof PhpPsiElement) {
            PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(phpDocAttributeList, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocString));
            if(childrenOfType instanceof StringLiteralExpression) {
                String contents = StringUtils.stripStart(((StringLiteralExpression) childrenOfType).getContents(), "\\");
                if(StringUtils.isNotBlank(contents) && contents.length() < 350) {
                    return contents;
                }
            }
        }

        return EventDispatcherUtil.extractEventClassInstance(phpDocComment.getText());
    }
}



