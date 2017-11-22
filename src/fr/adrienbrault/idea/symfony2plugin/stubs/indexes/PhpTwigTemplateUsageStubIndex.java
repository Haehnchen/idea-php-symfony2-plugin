package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTwigTemplateUsageStubIndex extends FileBasedIndexExtension<String, TemplateUsage> {

    public static final ID<String, TemplateUsage> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_php_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static int MAX_FILE_BYTE_SIZE = 2097152;
    private static ObjectStreamDataExternalizer<TemplateUsage> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    public static Set<String> RENDER_METHODS = new HashSet<String>() {{
        add("render");
        add("renderView");
        add("renderResponse");
    }};

    @NotNull
    @Override
    public ID<String, TemplateUsage> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, TemplateUsage, FileContent> getIndexer() {
        return new DataIndexer<String, TemplateUsage, FileContent>() {
            @NotNull
            @Override
            public Map<String, TemplateUsage> map(@NotNull FileContent inputData) {
                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return Collections.emptyMap();
                }

                if(!(inputData.getPsiFile() instanceof PhpFile) && isValidForIndex(inputData)) {
                    return Collections.emptyMap();
                }

                Map<String, Set<String>> items = new HashMap<>();

                psiFile.accept(new PsiRecursiveElementWalkingVisitor() {

                    @Override
                    public void visitElement(PsiElement element) {
                        if(element instanceof MethodReference) {
                            visitMethodReference((MethodReference) element);
                        } else if(element instanceof PhpDocTag) {
                            visitPhpDocTag((PhpDocTag) element);
                        }
                        super.visitElement(element);
                    }

                    private void visitMethodReference(@NotNull MethodReference methodReference) {
                        String methodName = methodReference.getName();
                        if(!RENDER_METHODS.contains(methodName)) {
                            return;
                        }

                        PsiElement[] parameters = methodReference.getParameters();
                        if(parameters.length == 0 || !(parameters[0] instanceof StringLiteralExpression)) {
                            return;
                        }

                        String contents = ((StringLiteralExpression) parameters[0]).getContents();
                        if(StringUtils.isBlank(contents) || !contents.endsWith(".html.twig")) {
                            return;
                        }

                        Function parentOfType = PsiTreeUtil.getParentOfType(methodReference, Function.class);
                        if(parentOfType == null) {
                            return;
                        }

                        addTemplateWithScope(contents, StringUtils.stripStart(parentOfType.getFQN(), "\\"));
                    }

                    /**
                     * "@Template("foobar.html.twig")"
                     * "@Template(template="foobar.html.twig")"
                     */
                    private void visitPhpDocTag(@NotNull PhpDocTag phpDocTag) {
                        // "@var" and user non related tags dont need an action
                        if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                            return;
                        }

                        // init scope imports
                        Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(phpDocTag);
                        if(fileImports.size() == 0) {
                            return;
                        }

                        String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
                        if(!"Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template".equals(StringUtils.stripStart(annotationFqnName, "\\"))) {
                            return;
                        }

                        String template = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
                        if(template != null && template.endsWith(".html.twig")) {
                            Method methodScope = AnnotationBackportUtil.getMethodScope(phpDocTag);
                            if(methodScope != null) {
                                addTemplateWithScope(template, StringUtils.stripStart(methodScope.getFQN(), "\\"));
                            }
                        }
                    }

                    private void addTemplateWithScope(@NotNull String contents, @NotNull String fqn) {
                        String s = TwigHelper.normalizeTemplateName(contents);
                        if(!items.containsKey(s)) {
                            items.put(s, new HashSet<>());
                        }

                        items.get(s).add(fqn);
                    }
                });

                Map<String, TemplateUsage> map = new HashMap<>();

                items.forEach(
                    (key, value) -> map.put(key, new TemplateUsage(key, value))
                );

                return map;
            }
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<TemplateUsage> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }


    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 3;
    }

    public static boolean isValidForIndex(FileContent inputData) {
        return inputData.getFile().getLength() < MAX_FILE_BYTE_SIZE;
    }

}



