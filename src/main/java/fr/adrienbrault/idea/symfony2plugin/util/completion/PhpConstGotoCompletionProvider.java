package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpVariantsUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class PhpConstGotoCompletionProvider extends GotoCompletionProvider {

    private static final String[] SPECIAL_STUB_CONSTANTS = new String[] {"true","false","null"};

    public PhpConstGotoCompletionProvider(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        PhpIndex phpIndex = PhpIndex.getInstance(this.getProject());
        Collection<LookupElement> constants = new ArrayList<>();

        for (String constantName : phpIndex.getAllConstantNames(null)) {
            if (Arrays.asList(SPECIAL_STUB_CONSTANTS).contains(constantName)) {
                continue;
            }
            constants.addAll(PhpVariantsUtil.getLookupItems(phpIndex.getConstantsByName(constantName), true, null));
        }

        return constants;
    }
}
