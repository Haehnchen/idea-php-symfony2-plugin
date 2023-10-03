package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  public function configureOptions(OptionsResolver $resolver)
 *  {
 *     $resolver->setDefaults(['data_class' => XXX]);
 *     $resolver->setDefault('data_class', XXX);
 *  }
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormDataClassStubIndex extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.form_data_class");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Set<String>> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile)) {
                return map;
            }

            for (PhpNamedElement topLevelElement : ((PhpFile) psiFile).getTopLevelDefs().values()) {
                if (topLevelElement instanceof PhpClass clazz) {
                    for (Method method : clazz.getOwnMethods()) {
                        PhpControlFlowUtil.processFlow(method.getControlFlow(), new PhpInstructionProcessor() {
                            @Override
                            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                                    visitElement(methodReference, map);
                                }
                                return super.processPhpCallInstruction(instruction);
                            }
                        });
                    }
                }
            }

            return map;
        };

    }
    public void visitElement(@NotNull MethodReference element, @NotNull Map<String, Set<String>> map) {
        String phpClassFqn = null;

        String name = element.getName();
        if ("setDefault".equals(name) && element.getType().getTypes().stream().anyMatch(s -> s.toLowerCase().contains("optionsresolver"))) {
            // $resolver->setDefault('data_class', XXX);

            ParameterList parameterList = element.getParameterList();
            if (parameterList != null) {
                PsiElement parameter = parameterList.getParameter(0);
                if (parameter instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) parameter).getContents();
                    if ("data_class".equals(contents)) {
                        PsiElement parameter1 = parameterList.getParameter(1);
                        if (parameter1 != null) {
                            phpClassFqn = getString(parameter1);
                        }
                    }
                }
            }
        } else if ("setDefaults".equals(name) && element.getType().getTypes().stream().anyMatch(s -> s.toLowerCase().contains("optionsresolver"))) {
            // $resolver->setDefaults(['data_class' => XXX]);

            ParameterList parameterList = ((MethodReference) element).getParameterList();
            if (parameterList != null) {
                PsiElement parameter = parameterList.getParameter(0);
                if (parameter instanceof ArrayCreationExpression) {
                    PhpPsiElement dataClassPsiElement = PhpElementsUtil.getArrayValue((ArrayCreationExpression) parameter, "data_class");
                    if (dataClassPsiElement != null) {
                        phpClassFqn = getString(dataClassPsiElement);
                    }
                }
            }
        }

        if (phpClassFqn != null) {
            Method methodScope = PsiTreeUtil.getParentOfType(element, Method.class);
            if (methodScope != null) {
                PhpClass parentOfType = methodScope.getContainingClass();
                if (parentOfType != null) {
                    map.putIfAbsent(phpClassFqn, new HashSet<>());
                    map.get(phpClassFqn).add(parentOfType.getFQN());
                }
            }
        }
    }

    @Nullable
    private static String getString(@NotNull PsiElement parameter) {
        if (parameter instanceof ClassConstantReference) {
            String classConstantPhpFqn = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) parameter);
            if (StringUtils.isNotBlank(classConstantPhpFqn)) {
                return "\\" + StringUtils.stripStart(classConstantPhpFqn, "\\");
            }
        } else if (parameter instanceof StringLiteralExpression) {
            String contents1 = ((StringLiteralExpression) parameter).getContents();
            if (StringUtils.isNotBlank(contents1)) {
                return "\\" + StringUtils.stripStart(contents1, "\\");
            }
        }

        return null;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return new StringSetDataExternalizer();
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
