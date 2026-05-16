package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil;

import java.util.List;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil
 */
public class FileLineMarkerUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testCreateLineMarkerInfoForFileUsesFirstContentLineAnchor() {
        String content = "\n\n/*\n" +
            " * boot\n" +
            " */\n" +
            "console.log('ok');\n";
        PsiFile psiFile = myFixture.addFileToProject("assets/file_marker.js", content);

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(List.of(psiFile))
            .setTooltipText("Navigate");

        RelatedItemLineMarkerInfo<PsiElement> markerInfo = FileLineMarkerUtil.createLineMarkerInfo(builder, psiFile);
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);

        assertNotNull(document);
        assertEquals("Navigate", markerInfo.getLineMarkerTooltip());
        assertFalse(markerInfo.getElement() instanceof PsiFile);

        int expectedStartOffset = content.indexOf("/*");
        int expectedLineNumber = document.getLineNumber(expectedStartOffset);
        assertEquals(expectedStartOffset, markerInfo.startOffset);
        assertTrue(markerInfo.endOffset > markerInfo.startOffset);
        assertEquals(expectedLineNumber, document.getLineNumber(markerInfo.startOffset));
        assertEquals(expectedLineNumber, document.getLineNumber(markerInfo.endOffset - 1));
        assertTrue(markerInfo.endOffset <= document.getLineEndOffset(expectedLineNumber));
    }
}
