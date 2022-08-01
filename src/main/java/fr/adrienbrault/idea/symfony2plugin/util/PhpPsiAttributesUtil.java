package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Helpers for PHP 8 Attributes psi access
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpPsiAttributesUtil {
    @Nullable
    public static String getAttributeValueByNameAsString(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        PsiElement nextSibling = findAttributeByName(attribute, attributeName);

        if (nextSibling instanceof StringLiteralExpression) {
            String contents = ((StringLiteralExpression) nextSibling).getContents();
            if (StringUtils.isNotBlank(contents)) {
                return contents;
            }
        } else if(nextSibling instanceof ClassConstantReference) {
            return resolveLocalValue(attribute, (ClassConstantReference) nextSibling);
        }

        return null;
    }

    @NotNull
    public static Collection<String> getAttributeValueByNameAsArray(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        PsiElement nextSibling = findAttributeByName(attribute, attributeName);

        if (nextSibling instanceof ArrayCreationExpression) {
            return PhpElementsUtil.getArrayValuesAsString((ArrayCreationExpression) nextSibling);
        }

        return Collections.emptyList();
    }

    @NotNull
    public static Collection<String> getAttributeValueByNameAsArrayLocalResolve(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        PsiElement nextSibling = findAttributeByName(attribute, attributeName);

        Collection<String> values = new HashSet<>();
        if (nextSibling instanceof ArrayCreationExpression) {
            for (PsiElement arrayValue : PhpElementsUtil.getArrayValues((ArrayCreationExpression) nextSibling)) {
                if (arrayValue instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) arrayValue).getContents();
                    if (StringUtils.isNotBlank(contents)) {
                        values.add(contents);
                    }
                } else if(arrayValue instanceof ClassConstantReference) {
                    String contents = resolveLocalValue(attribute, (ClassConstantReference) arrayValue);
                    if (StringUtils.isNotBlank(contents)) {
                        values.add(contents);
                    }
                }
            }
        }

        return values;
    }

    /**
     * find default "#[Route(path: '/attributesWithoutName')]" or "#[Route('/attributesWithoutName')]"
     */
    @Nullable
    public static String getAttributeValueByNameAsStringWithDefaultParameterFallback(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        String pathAttribute = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, attributeName);
        if (StringUtils.isNotBlank(pathAttribute)) {
            return pathAttribute;
        }

        // find default "#[Route('/attributesWithoutName')]"
        for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
            PhpExpectedFunctionArgument argument1 = argument.getArgument();
            if (argument1.getArgumentIndex() == 0) {
                // what a mess here :)
                // createReference is stopping with exception on array values
                String value = argument1.getValue();
                String s = org.apache.commons.lang3.StringUtils.normalizeSpace(value).replaceAll("[\\n\\t ]", "");
                if (s.startsWith("['") || s.startsWith("[\"")) {
                    continue;
                }

                // hint: reference is a complete fake object lazily created; it is not reflect the real element :(
                // PhpPsiElementFactory.createPhpPsiFromText => com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionClassConstantArgument.createReference
                @NotNull PsiElement namedElement;
                try {
                    namedElement = argument1.createReference(attribute.getProject());
                } catch (AssertionError e) {
                    continue;
                }

                if (namedElement instanceof StringLiteralExpression) {
                    // we can trust the string representation here, looks to be right implemented
                    String contents = ((StringLiteralExpression) namedElement).getContents();
                    if (StringUtils.isNotBlank(contents)) {
                        return contents;
                    }
                } else if(namedElement instanceof ClassConstantReference) {
                    // not working: we are in a dummy and temporary out-of-scope object
                    // resolveLocalValue(attribute, (ClassConstantReference) nextSibling);
                    PhpExpression classReference = ((ClassConstantReference) namedElement).getClassReference();
                    if (classReference instanceof ClassReference) {
                        PsiElement phpAttributesList = attribute.getParent();
                        if (phpAttributesList instanceof PhpAttributesList) {
                            PsiElement method = phpAttributesList.getParent();
                            if (method instanceof Method) {
                                PsiElement phpClass = method.getParent();
                                // instead of normal "$this", "self", ... we are getting here the class name :(
                                // so to check the owning scope compare it via the class name
                                if (phpClass instanceof PhpClass && ((PhpClass) phpClass).getFQN().equals(((ClassReference) classReference).getFQN())) {
                                    String fieldName = ((ClassConstantReference) namedElement).getName();
                                    Field ownFieldByName = ((PhpClass) phpClass).findOwnFieldByName(fieldName, true);
                                    if (ownFieldByName != null) {
                                        PsiElement defaultValue = ownFieldByName.getDefaultValue();
                                        if (defaultValue instanceof StringLiteralExpression) {
                                            String contents = ((StringLiteralExpression) defaultValue).getContents();
                                            if (StringUtils.isNotBlank(contents)) {
                                                return contents;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Workaround to find given attribute: "#[Route('/attributesWithoutName', name: "")]" as attribute iteration given the index as "int" but not the key as name
     */
    @Nullable
    private static PsiElement findAttributeByName(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        ParameterList parameterList = PsiTreeUtil.findChildOfType(attribute, ParameterList.class);
        if (parameterList == null) {
            return null;
        }

        Collection<PsiElement> childrenOfTypeAsList = PsiElementUtils.getChildrenOfTypeAsList(parameterList, getAttributeColonPattern(attributeName));

        if (childrenOfTypeAsList.isEmpty()) {
            return null;
        }

        PsiElement colon = childrenOfTypeAsList.iterator().next();

        return PhpPsiUtil.getNextSibling(colon, psiElement -> psiElement instanceof PsiWhiteSpace);
    }

    /**
     * "#[Route('/path', name: 'attributes_action')]"
     */
    @NotNull
    private static PsiElementPattern.Capture<PsiElement> getAttributeColonPattern(String name) {
        return PlatformPatterns.psiElement().withElementType(
            PhpTokenTypes.opCOLON
        ).afterLeaf(PlatformPatterns.psiElement().withElementType(PhpTokenTypes.IDENTIFIER).withText(name));
    }

    @Nullable
    private static String resolveLocalValue(@NotNull PhpAttribute attribute, @NotNull ClassConstantReference nextSibling) {
        PhpExpression classReference = nextSibling.getClassReference();
        if (classReference != null) {
            String name = classReference.getName();
            if (name != null && (name.equals("self") || name.equals("static"))) {
                PsiElement phpAttributesList = attribute.getParent();
                if (phpAttributesList instanceof PhpAttributesList) {
                    PsiElement method = phpAttributesList.getParent();
                    if (method instanceof Method) {
                        PsiElement phpClass = method.getParent();
                        if (phpClass instanceof PhpClass) {
                            String fieldName = nextSibling.getName();
                            Field ownFieldByName = ((PhpClass) phpClass).findOwnFieldByName(fieldName, true);
                            if (ownFieldByName != null) {
                                PsiElement defaultValue = ownFieldByName.getDefaultValue();
                                if (defaultValue instanceof StringLiteralExpression) {
                                    String contents = ((StringLiteralExpression) defaultValue).getContents();
                                    if (StringUtils.isNotBlank(contents)) {
                                        return contents;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
