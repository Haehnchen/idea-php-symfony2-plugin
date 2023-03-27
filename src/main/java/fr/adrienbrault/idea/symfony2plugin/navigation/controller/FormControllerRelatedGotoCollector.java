package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {
    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        Collection<PhpClass> uniqueTargets = new HashSet<>();

        Collection<MethodReference> methodReferences = new ArrayList<>();

        PhpControlFlowUtil.processFlow(parameter.getMethod().getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                    methodReferences.add(methodReference);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });

        for (MethodReference methodReference : methodReferences) {
            PsiElement parameter0 = methodReference.getParameter(0);
            if (parameter0 == null) {
                continue;
            }

            MethodMatcher.MethodMatchParameter matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(parameter0, FormUtil.FORM_FACTORY_SIGNATURES);
            if (matchedSignature != null) {
                PhpClass formTypeToClass = FormUtil.getFormTypeClassOnParameter(parameter0);
                if (formTypeToClass != null) {
                    uniqueTargets.add(formTypeToClass);
                }
            }

            PsiElement parameter1 = methodReference.getParameter(1);
            if (parameter1 != null) {
                MethodMatcher.MethodMatchParameter matchedSignature1 = MethodMatcher.getMatchedSignatureWithDepth(parameter1, FormUtil.PHP_FORM_NAMED_BUILDER_SIGNATURES, 1);
                if (matchedSignature1 != null) {
                    PhpClass formTypeToClass = FormUtil.getFormTypeClassOnParameter(parameter1);
                    if (formTypeToClass != null) {
                        uniqueTargets.add(formTypeToClass);
                    }
                }
            }

        }

        for (PhpClass phpClass : uniqueTargets) {
            parameter.add(new RelatedPopupGotoLineMarker
                .PopupGotoRelatedItem(phpClass, StringUtils.stripStart(phpClass.getFQN(), "\\"))
                .withIcon(Symfony2Icons.FORM_TYPE, Symfony2Icons.FORM_TYPE_LINE_MARKER)
            );
        }
    }
}
