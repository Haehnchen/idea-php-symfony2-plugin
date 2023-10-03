package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import kotlin.Triple;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTwigTemplateUsageStubIndex extends FileBasedIndexExtension<String, TemplateUsage> {

    public static final ID<String, TemplateUsage> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_php_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final int MAX_FILE_BYTE_SIZE = 2097152;
    private static final ObjectStreamDataExternalizer<TemplateUsage> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, TemplateUsage> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, TemplateUsage, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            if(!(inputData.getPsiFile() instanceof PhpFile) && isValidForIndex(inputData)) {
                return Collections.emptyMap();
            }

            Map<String, Set<String>> items = new HashMap<>();
            Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer = triple -> {
                String templateName = triple.getFirst();

                if (templateName.length() > 255) {
                    return;
                }

                if(!items.containsKey(templateName)) {
                    items.put(templateName, new HashSet<>());
                }

                items.get(templateName).add(StringUtils.stripStart(triple.getSecond().getFQN(), "\\"));
            };

            @NotNull NotNullLazyValue<Set<String>> methods = PhpMethodVariableResolveUtil.TemplateRenderVisitor.createLazyMethodNamesCollector(psiFile.getProject());
            for (PhpNamedElement topLevelElement : ((PhpFile) psiFile).getTopLevelDefs().values()) {
                if (topLevelElement instanceof PhpClass clazz) {
                    for (Method method : clazz.getOwnMethods()) {
                        PhpMethodVariableResolveUtil.TemplateRenderVisitor.processMethodAttributes(method, consumer);
                        PhpDocComment docComment = method.getDocComment();
                        if (docComment != null) {
                            PhpDocUtil.processTagElementsByName(docComment, null, docTag -> {
                                PhpMethodVariableResolveUtil.TemplateRenderVisitor.processDocTag(docTag, consumer);
                                return true;
                            });
                        }
                        processMethodReferences(consumer, methods, method);
                    }
                }
                if (topLevelElement instanceof Function function) {
                    processMethodReferences(consumer, methods, function);
                }
            }

            Map<String, TemplateUsage> map = new HashMap<>();

            items.forEach(
                (key, value) -> map.put(key, new TemplateUsage(key, value))
            );

            return map;
        };
    }

    private static void processMethodReferences(@NotNull Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer, @NotNull NotNullLazyValue<Set<String>> methods, @NotNull Function function) {
        PhpControlFlowUtil.processFlow(function.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                    PhpMethodVariableResolveUtil.TemplateRenderVisitor.processMethodReference(methodReference, methods, consumer);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });
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

    private static boolean isValidForIndex(FileContent inputData) {
        return inputData.getFile().getLength() < MAX_FILE_BYTE_SIZE;
    }
}



