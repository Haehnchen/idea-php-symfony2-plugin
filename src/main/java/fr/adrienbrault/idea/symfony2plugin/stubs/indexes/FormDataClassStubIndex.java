package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            Map<String, Set<String>> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile)) {
                return map;
            }

            psiFile.accept(new PsiRecursiveElementVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (element instanceof MethodReference) {
                        String phpClassFqn = null;

                        String name = ((MethodReference) element).getName();
                        if ("setDefault".equals(name) && ((MethodReference) element).getType().getTypes().stream().anyMatch(s -> s.toLowerCase().contains("optionsresolver"))) {
                            // $resolver->setDefault('data_class', XXX);

                            ParameterList parameterList = ((MethodReference) element).getParameterList();
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
                        } else if ("setDefaults".equals(name) && ((MethodReference) element).getType().getTypes().stream().anyMatch(s -> s.toLowerCase().contains("optionsresolver"))) {
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

                    super.visitElement(element);
                }

                @Nullable
                private String getString(@NotNull PsiElement parameter) {
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
            });

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
