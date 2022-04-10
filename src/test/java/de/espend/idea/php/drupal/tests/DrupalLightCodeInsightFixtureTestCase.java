package de.espend.idea.php.drupal.tests;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.codeInspection.*;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.Annotator;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.ID;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpReference;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public abstract class DrupalLightCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Settings.getInstance(myFixture.getProject()).pluginEnabled = true;
    }

    public void assertCompletionContains(LanguageFileType languageFileType, String configureByText, String... lookupStrings) {

        myFixture.configureByText(languageFileType, configureByText);
        myFixture.completeBasic();

        checkContainsCompletion(lookupStrings);
    }

    public void assertCompletionNotContains(String text, String configureByText, String... lookupStrings) {

        myFixture.configureByText(text, configureByText);
        myFixture.completeBasic();

        assertFalse(myFixture.getLookupElementStrings().containsAll(Arrays.asList(lookupStrings)));
    }

    public void assertCompletionNotContains(LanguageFileType languageFileType, String configureByText, String... lookupStrings) {

        myFixture.configureByText(languageFileType, configureByText);
        myFixture.completeBasic();

        assertFalse(myFixture.getLookupElementStrings().containsAll(Arrays.asList(lookupStrings)));
    }

    public void assertCompletionContains(String filename, String configureByText, String... lookupStrings) {

        myFixture.configureByText(filename, configureByText);
        myFixture.completeBasic();

        completionContainsAssert(lookupStrings);
    }

    private void completionContainsAssert(String[] lookupStrings) {
        List<String> lookupElements = myFixture.getLookupElementStrings();
        if(lookupElements == null) {
            fail(String.format("failed that empty completion contains %s", Arrays.toString(lookupStrings)));
        }

        for (String s : Arrays.asList(lookupStrings)) {
            if(!lookupElements.contains(s)) {
                fail(String.format("failed that completion contains %s in %s", s, lookupElements.toString()));
            }
        }
    }

    public void assertNavigationContains(LanguageFileType languageFileType, String configureByText, String targetShortcut) {
        myFixture.configureByText(languageFileType, configureByText);
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNavigationContains(psiElement, targetShortcut);
    }

    public void assertNavigationContains(PsiElement psiElement, String targetShortcut) {

        if(!targetShortcut.startsWith("\\")) {
            targetShortcut = "\\" + targetShortcut;
        }

        Set<String> classTargets = new HashSet<String>();

        for (GotoDeclarationHandler gotoDeclarationHandler : Extensions.getExtensions(GotoDeclarationHandler.EP_NAME)) {
            PsiElement[] gotoDeclarationTargets = gotoDeclarationHandler.getGotoDeclarationTargets(psiElement, 0, myFixture.getEditor());
            if(gotoDeclarationTargets != null && gotoDeclarationTargets.length > 0) {

                for (PsiElement gotoDeclarationTarget : gotoDeclarationTargets) {
                    if(gotoDeclarationTarget instanceof Method) {

                        String meName = ((Method) gotoDeclarationTarget).getName();

                        String clName = ((Method) gotoDeclarationTarget).getContainingClass().getPresentableFQN();
                        if(!clName.startsWith("\\")) {
                            clName = "\\" + clName;
                        }

                        classTargets.add(clName + "::" + meName);
                    } else if(gotoDeclarationTarget instanceof Function) {
                        classTargets.add("\\" + ((Function) gotoDeclarationTarget).getName());
                    }
                }

            }
        }

        if(!classTargets.contains(targetShortcut)) {
            fail(String.format("failed that PsiElement (%s) navigate to %s on %s", psiElement.toString(), targetShortcut, classTargets.toString()));
        }

    }

    public void assertNavigationMatch(String filename, String configureByText, ElementPattern<?> pattern) {
        myFixture.configureByText(filename, configureByText);
        assertNavigationMatch(pattern);
    }

    public void assertNavigationMatch(LanguageFileType languageFileType, String configureByText, ElementPattern<?> pattern) {
        myFixture.configureByText(languageFileType, configureByText);
        assertNavigationMatch(pattern);
    }

    public void assertNavigationMatch(LanguageFileType languageFileType, String configureByText) {
        myFixture.configureByText(languageFileType, configureByText);
        assertNavigationMatch(PlatformPatterns.psiElement());
    }

    public void assertNavigationMatch(String filename, String configureByText) {
        myFixture.configureByText(filename, configureByText);
        assertNavigationMatch(PlatformPatterns.psiElement());
    }

    private void assertNavigationIsEmpty() {
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        for (GotoDeclarationHandler gotoDeclarationHandler : Extensions.getExtensions(GotoDeclarationHandler.EP_NAME)) {
            PsiElement[] gotoDeclarationTargets = gotoDeclarationHandler.getGotoDeclarationTargets(psiElement, 0, myFixture.getEditor());
            if(gotoDeclarationTargets != null && gotoDeclarationTargets.length > 0) {
                fail(String.format("failed that PsiElement (%s) navigate is empty; found target in '%s'", psiElement.toString(), gotoDeclarationHandler.getClass()));
            }
        }
    }

    private void assertNavigationMatch(ElementPattern<?> pattern) {

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Set<String> targetStrings = new HashSet<String>();

        for (GotoDeclarationHandler gotoDeclarationHandler : Extensions.getExtensions(GotoDeclarationHandler.EP_NAME)) {

            PsiElement[] gotoDeclarationTargets = gotoDeclarationHandler.getGotoDeclarationTargets(psiElement, 0, myFixture.getEditor());
            if(gotoDeclarationTargets == null || gotoDeclarationTargets.length == 0) {
                continue;
            }

            for (PsiElement gotoDeclarationTarget : gotoDeclarationTargets) {
                targetStrings.add(gotoDeclarationTarget.toString());
                if(pattern.accepts(gotoDeclarationTarget)) {
                    return;
                }
            }
        }

        fail(String.format("failed that PsiElement (%s) navigate matches one of %s", psiElement.toString(), targetStrings.toString()));
    }

    public void assertCompletionResultEquals(String filename, String complete, String result) {
        myFixture.configureByText(filename, complete);
        myFixture.completeBasic();
        myFixture.checkResult(result);
    }

    public void assertCompletionResultEquals(@NotNull FileType fileType, @NotNull String contents, @NotNull String result, @NotNull SymfonyLightCodeInsightFixtureTestCase.LookupElementInsert.Assert assertion) {
        myFixture.configureByText(fileType, contents);
        UIUtil.invokeAndWaitIfNeeded(new MyLookupElementConditionalInsertRunnable(assertion));
        myFixture.checkResult(result);
    }

    public void assertCompletionResultEquals(LanguageFileType languageFileType, String complete, String result) {
        myFixture.configureByText(languageFileType, complete);
        myFixture.completeBasic();
        myFixture.checkResult(result);
    }

    public void assertIndexContains(@NotNull ID<String, ?> id, @NotNull String... keys) {
        assertIndex(id, false, keys);
    }

    public void assertIndex(@NotNull ID<String, ?> id, boolean notCondition, @NotNull String... keys) {
        for (String key : keys) {

            final Collection<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();

            FileBasedIndexImpl.getInstance().getFilesWithKey(id, new HashSet<String>(Arrays.asList(key)), new Processor<VirtualFile>() {
                @Override
                public boolean process(VirtualFile virtualFile) {
                    virtualFiles.add(virtualFile);
                    return true;
                }
            }, GlobalSearchScope.allScope(getProject()));

            if(notCondition && virtualFiles.size() > 0) {
                fail(String.format("Fail that ID '%s' not contains '%s'", id.toString(), key));
            } else if(!notCondition && virtualFiles.size() == 0) {
                fail(String.format("Fail that ID '%s' contains '%s'", id.toString(), key));
            }
        }
    }

    public void assertIndexContainsKeyWithValue(@NotNull ID<String, String> id, @NotNull String key, @NotNull String value) {
        assertContainsElements(FileBasedIndexImpl.getInstance().getValues(id, key, GlobalSearchScope.allScope(getProject())), value);
    }

    public <T> void assertIndexContainsKeyWithValue(@NotNull ID<String, T> id, @NotNull String key, @NotNull IndexValue.Assert<T> tAssert) {
        List<T> values = FileBasedIndexImpl.getInstance().getValues(id, key, GlobalSearchScope.allScope(getProject()));
        for (T t : values) {
            if(tAssert.match(t)) {
                return;
            }
        }

        fail(String.format("Fail that Key '%s' matches on of '%s' values", key, values.size()));
    }

    public void assertLocalInspectionContains(String filename, String content, String contains) {
        Set<String> matches = new HashSet<String>();

        Pair<List<ProblemDescriptor>, Integer> localInspectionsAtCaret = getLocalInspectionsAtCaret(filename, content);
        for (ProblemDescriptor result : localInspectionsAtCaret.getFirst()) {
            TextRange textRange = result.getPsiElement().getTextRange();
            if (textRange.contains(localInspectionsAtCaret.getSecond()) && result.toString().equals(contains)) {
                return;
            }

            matches.add(result.toString());
        }

        fail(String.format("Fail matches '%s' with one of %s", contains, matches));
    }

    public void assertAnnotationContains(String filename, String content, String contains) {
        List<String> matches = new ArrayList<String>();
        for (Annotation annotation : getAnnotationsAtCaret(filename, content)) {
            matches.add(annotation.toString());
            if(annotation.getMessage().contains(contains)) {
                return;
            }
        }

        fail(String.format("Fail matches '%s' with one of %s", contains, matches));
    }

    @NotNull
    private AnnotationHolderImpl getAnnotationsAtCaret(String filename, String content) {
        PsiFile psiFile = myFixture.configureByText(filename, content);
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        AnnotationHolderImpl annotations = new AnnotationHolderImpl(new AnnotationSession(psiFile));

        for (Annotator annotator : LanguageAnnotators.INSTANCE.allForLanguage(psiFile.getLanguage())) {
            annotator.annotate(psiElement, annotations);
        }

        return annotations;
    }

    public void assertAnnotationNotContains(String filename, String content, String contains) {
        for (Annotation annotation : getAnnotationsAtCaret(filename, content)) {
            if(annotation.getMessage().contains(contains)) {
                fail(String.format("Fail not matching '%s' with '%s'", contains, annotation));
            }
        }
    }

    public void assertIntentionIsAvailable(LanguageFileType languageFileType, String configureByText, String intentionText) {
        myFixture.configureByText(languageFileType, configureByText);
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        for (IntentionAction intentionAction : IntentionManager.getInstance().getIntentionActions()) {
            if(intentionAction.isAvailable(getProject(), getEditor(), psiElement.getContainingFile()) && intentionAction.getText().equals(intentionText)) {
                return;
            }
        }

        fail(String.format("Fail intention action '%s' is available in element '%s'", intentionText, psiElement.getText()));
    }

    public void assertLocalInspectionNotContains(String filename, String content, String contains) {
        Pair<List<ProblemDescriptor>, Integer> localInspectionsAtCaret = getLocalInspectionsAtCaret(filename, content);

        for (ProblemDescriptor result : localInspectionsAtCaret.getFirst()) {
            TextRange textRange = result.getPsiElement().getTextRange();
            if (textRange.contains(localInspectionsAtCaret.getSecond()) && result.toString().contains(contains)) {
                fail(String.format("Fail inspection not contains '%s'", contains));
            }
        }
    }

    private Pair<List<ProblemDescriptor>, Integer> getLocalInspectionsAtCaret(String filename, String content) {

        PsiElement psiFile = myFixture.configureByText(filename, content);

        int caretOffset = myFixture.getCaretOffset();
        if(caretOffset <= 0) {
            fail("Please provide <caret> tag");
        }

        ProblemsHolder problemsHolder = new ProblemsHolder(InspectionManager.getInstance(getProject()), psiFile.getContainingFile(), false);

        for (LocalInspectionEP localInspectionEP : LocalInspectionEP.LOCAL_INSPECTION.getExtensions()) {
            Object object = localInspectionEP.getInstance();
            if(!(object instanceof LocalInspectionTool)) {
                continue;
            }

            final PsiElementVisitor psiElementVisitor = ((LocalInspectionTool) object).buildVisitor(problemsHolder, false);

            psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    psiElementVisitor.visitElement(element);
                    super.visitElement(element);
                }
            });

            psiElementVisitor.visitFile(psiFile.getContainingFile());;
        }

        return new Pair<List<ProblemDescriptor>, Integer>(problemsHolder.getResults(), caretOffset);
    }

    protected void assertLocalInspectionIsEmpty(String filename, String content) {
        Pair<List<ProblemDescriptor>, Integer> localInspectionsAtCaret = getLocalInspectionsAtCaret(filename, content);

        for (ProblemDescriptor result : localInspectionsAtCaret.getFirst()) {
            TextRange textRange = result.getPsiElement().getTextRange();
            if (textRange.contains(localInspectionsAtCaret.getSecond())) {
                fail("Fail that matches is empty");
            }
        }
    }

    protected void createDummyFiles(String... files) throws Exception {
        for (String file : files) {
            String path = myFixture.getProject().getBaseDir().getPath() + "/" + file;
            File f = new File(path);
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
    }

    private void checkContainsCompletion(String[] lookupStrings) {
        completionContainsAssert(lookupStrings);
    }

    public void assertLineMarker(@NotNull PsiElement psiElement, @NotNull LineMarker.Assert assertMatch) {

        final List<PsiElement> elements = collectPsiElementsRecursive(psiElement);

        for (LineMarkerProvider lineMarkerProvider : LineMarkerProviders.getInstance().allForLanguage(psiElement.getLanguage())) {
            Collection<LineMarkerInfo> lineMarkerInfos = new ArrayList<LineMarkerInfo>();
            lineMarkerProvider.collectSlowLineMarkers(elements, lineMarkerInfos);

            if(lineMarkerInfos.size() == 0) {
                continue;
            }

            for (LineMarkerInfo lineMarkerInfo : lineMarkerInfos) {
                if(assertMatch.match(lineMarkerInfo)) {
                    return;
                }
            }
        }

        fail(String.format("Fail that '%s' matches on of '%s' PsiElements", assertMatch.getClass(), elements.size()));
    }

    @NotNull
    private List<PsiElement> collectPsiElementsRecursive(@NotNull PsiElement psiElement) {
        final List<PsiElement> elements = new ArrayList<PsiElement>();
        elements.add(psiElement.getContainingFile());

        psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                elements.add(element);
                super.visitElement(element);
            }
        });
        return elements;
    }

    public static class IndexValue {
        public interface Assert<T> {
            boolean match(@NotNull T value);
        }
    }

    public static class LineMarker {
        public interface Assert {
            boolean match(@NotNull LineMarkerInfo markerInfo);
        }

        public static class ToolTipEqualsAssert implements Assert {
            @NotNull
            private final String toolTip;

            public ToolTipEqualsAssert(@NotNull String toolTip) {
                this.toolTip = toolTip;
            }

            @Override
            public boolean match(@NotNull LineMarkerInfo markerInfo) {
                return markerInfo.getLineMarkerTooltip() != null && markerInfo.getLineMarkerTooltip().equals(toolTip);
            }
        }

        public static class TargetAcceptsPattern implements Assert {

            @NotNull
            private final String toolTip;
            @NotNull
            private final ElementPattern<? extends PsiElement> pattern;

            public TargetAcceptsPattern(@NotNull String toolTip, @NotNull ElementPattern<? extends PsiElement> pattern) {
                this.toolTip = toolTip;
                this.pattern = pattern;
            }

            @Override
            public boolean match(@NotNull LineMarkerInfo markerInfo) {
                if(markerInfo.getLineMarkerTooltip() == null || !markerInfo.getLineMarkerTooltip().equals(toolTip)) {
                    return false;
                }

                if(!(markerInfo instanceof RelatedItemLineMarkerInfo)) {
                    return false;
                }

                for (Object o : ((RelatedItemLineMarkerInfo) markerInfo).createGotoRelatedItems()) {
                    if(o instanceof GotoRelatedItem && this.pattern.accepts(((GotoRelatedItem) o).getElement())) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public static class LookupElementInsert {
        public interface Assert {
            boolean match(@NotNull LookupElement lookupElement);
        }

        public static class Icon implements Assert {

            @NotNull
            private final javax.swing.Icon icon;

            public Icon(@NotNull javax.swing.Icon icon) {
                this.icon = icon;
            }

            @Override
            public boolean match(@NotNull LookupElement lookupElement) {
                LookupElementPresentation presentation = new LookupElementPresentation();
                lookupElement.renderElement(presentation);
                return presentation.getIcon() == this.icon;
            }
        }
    }

    public static class LookupElementPresentationAssert {
        public interface Assert {
            boolean match(@NotNull LookupElementPresentation lookupElement);
        }
    }

    private class MyLookupElementConditionalInsertRunnable implements Runnable {

        @NotNull
        private final SymfonyLightCodeInsightFixtureTestCase.LookupElementInsert.Assert insert;

        public MyLookupElementConditionalInsertRunnable(@NotNull SymfonyLightCodeInsightFixtureTestCase.LookupElementInsert.Assert insert) {
            this.insert = insert;
        }

        @Override
        public void run() {
            CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
                @Override
                public void run() {
                    final CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(CompletionType.BASIC) {

                        @Override
                        protected void completionFinished(final CompletionProgressIndicator indicator, boolean hasModifiers) {

                            // find our lookup element
                            final LookupElement lookupElement = ContainerUtil.find(indicator.getLookup().getItems(), new Condition<LookupElement>() {
                                @Override
                                public boolean value(LookupElement lookupElement) {
                                    return insert.match(lookupElement);
                                }
                            });

                            if(lookupElement == null) {
                                fail("No matching lookup element found");
                            }

                            // overwrite behavior and force completion + insertHandler
                            CommandProcessor.getInstance().executeCommand(indicator.getProject(), new Runnable() {
                                @Override
                                public void run() {
                                    CommandProcessor.getInstance().setCurrentCommandGroupId("Completion" + indicator.hashCode());
                                    indicator.getLookup().finishLookup(Lookup.AUTO_INSERT_SELECT_CHAR, lookupElement);
                                }
                            }, "Autocompletion", null);
                        }
                    };

                    Editor editor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(getEditor(), getFile());
                    handler.invokeCompletion(getProject(), editor);
                    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
                }
            }, null, null);
        }
    }
}
