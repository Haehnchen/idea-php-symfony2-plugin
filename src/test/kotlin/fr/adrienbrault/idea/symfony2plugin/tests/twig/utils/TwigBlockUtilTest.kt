package fr.adrienbrault.idea.symfony2plugin.tests.twig.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigBlockUtil
 */
class TwigBlockUtilTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        )
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsFindsBlockInParent() {
        myFixture.addFileToProject("templates/base.html.twig", "{% block content %}base{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}{% block content %}child{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(childFile, "content", false)

        assertFalse("Should find block 'content' in parent", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsReturnsEmptyForUnknownBlock() {
        myFixture.addFileToProject("templates/base.html.twig", "{% block content %}base{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}{% block content %}child{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(childFile, "nonexistent", false)

        assertTrue("Should return empty for unknown block", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsWithSelfIncludesSelfBlocks() {
        myFixture.addFileToProject("templates/base.html.twig", "{% block content %}base{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}{% block content %}child{% endblock %}"
        )

        val withSelf: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(childFile, "content", true)
        val withoutSelf: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(childFile, "content", false)

        assertTrue("withSelf should have more or equal targets", withSelf.size >= withoutSelf.size)
        assertFalse("withSelf should include own block definition", withSelf.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockImplementationTargets
     */
    fun testGetBlockImplementationTargetsFindsChildOverride() {
        val baseFile: PsiFile = myFixture.addFileToProject(
            "templates/base.html.twig",
            "{% block content %}base{% endblock %}"
        )
        myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}{% block content %}child{% endblock %}"
        )

        val block = TwigUtil.getBlocksInFile(baseFile as TwigFile)
            .firstOrNull { it.name == "content" }

        assertNotNull("Block 'content' should exist in base template", block)

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockImplementationTargets(block!!.target)

        assertFalse("Should find child block implementation", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockImplementationTargets
     */
    fun testGetBlockImplementationTargetsReturnsEmptyWhenNoChildren() {
        val baseFile: PsiFile = myFixture.addFileToProject(
            "templates/base_standalone.html.twig",
            "{% block content %}base{% endblock %}"
        )

        val block = TwigUtil.getBlocksInFile(baseFile as TwigFile)
            .firstOrNull { it.name == "content" }

        assertNotNull("Block 'content' should exist", block)

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockImplementationTargets(block!!.target)

        assertTrue("Should return empty when no child extends this template", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getComponentBlockTargets
     */
    fun testGetComponentBlockTargetsFindsBlockInComponentTemplate() {
        myFixture.addFileToProject(
            "templates/components/Alert.html.twig",
            "{% block content %}alert content{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getComponentBlockTargets(project, "Alert", "content")

        assertFalse("Should find block 'content' in component template", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getComponentBlockTargets
     */
    fun testGetComponentBlockTargetsReturnsEmptyForUnknownBlock() {
        myFixture.addFileToProject(
            "templates/components/Alert.html.twig",
            "{% block content %}alert content{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getComponentBlockTargets(project, "Alert", "nonexistent")

        assertTrue("Should return empty for a block that does not exist in the component template", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getComponentBlockTargets
     */
    fun testGetComponentBlockTargetsReturnsEmptyForUnknownComponent() {
        val targets: Collection<PsiElement> = TwigBlockUtil.getComponentBlockTargets(project, "NonExistent", "content")

        assertTrue("Should return empty for a component that does not exist", targets.isEmpty())
    }

    /**
     * @see TwigUtil.getBlockNamesForFiles
     * @see TwigFileUtil.collectParentFiles
     */
    fun testGetBlockNamesForFilesReturnsBlockNames() {
        val file: PsiFile = myFixture.addFileToProject(
            "templates/base_names.html.twig",
            "{% block content %}base{% endblock %}{% block sidebar %}side{% endblock %}"
        )

        val names = TwigUtil.getBlockNamesForFiles(project, TwigFileUtil.collectParentFiles(true, file)).values.flatten().toSet()

        assertTrue("Should contain 'content'", names.contains("content"))
        assertTrue("Should contain 'sidebar'", names.contains("sidebar"))
    }

    /**
     * @see TwigUtil.getBlockNamesForFiles
     * @see TwigFileUtil.collectParentFiles
     */
    fun testGetBlockNamesForFilesWalksParentChain() {
        myFixture.addFileToProject("templates/base_chain.html.twig", "{% block header %}head{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child_chain.html.twig",
            "{% extends 'base_chain.html.twig' %}{% block content %}child{% endblock %}"
        )

        val names = TwigUtil.getBlockNamesForFiles(project, TwigFileUtil.collectParentFiles(true, childFile)).values.flatten().toSet()

        assertTrue("Should contain own block 'content'", names.contains("content"))
        assertTrue("Should contain parent block 'header'", names.contains("header"))
    }

    /**
     * @see TwigFileUtil.collectParentFiles
     */
    fun testCollectParentFilesWithoutSelfExcludesOwnFile() {
        myFixture.addFileToProject("templates/base_noself.html.twig", "{% block header %}head{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child_noself.html.twig",
            "{% extends 'base_noself.html.twig' %}{% block content %}child{% endblock %}"
        )

        val withSelf = TwigUtil.getBlockNamesForFiles(project, TwigFileUtil.collectParentFiles(true, childFile)).values.flatten().toSet()
        val withoutSelf = TwigUtil.getBlockNamesForFiles(project, TwigFileUtil.collectParentFiles(false, childFile)).values.flatten().toSet()

        assertTrue("withSelf should include own block", withSelf.contains("content"))
        assertFalse("withoutSelf should exclude own block", withoutSelf.contains("content"))
        assertTrue("Both should include parent block", withoutSelf.contains("header"))
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsByFileReturnsPsiTarget() {
        val file: PsiFile = myFixture.addFileToProject(
            "templates/base_targets.html.twig",
            "{% block content %}base{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(file, "content", true)

        assertFalse("Should find PSI target for 'content'", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsByFileReturnsEmptyForUnknownBlock() {
        val file: PsiFile = myFixture.addFileToProject(
            "templates/base_targets_unknown.html.twig",
            "{% block content %}base{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(file, "nonexistent", true)

        assertTrue("Should return empty for unknown block", targets.isEmpty())
    }

    /**
     * @see TwigBlockUtil.getBlockOverwriteTargets
     */
    fun testGetBlockOverwriteTargetsByFileWalksParentChain() {
        myFixture.addFileToProject("templates/base_resolve.html.twig", "{% block header %}head{% endblock %}")
        val childFile: PsiFile = myFixture.addFileToProject(
            "templates/child_resolve.html.twig",
            "{% extends 'base_resolve.html.twig' %}{% block content %}child{% endblock %}"
        )

        val targets: Collection<PsiElement> = TwigBlockUtil.getBlockOverwriteTargets(childFile, "header", true)

        assertFalse("Should find parent block 'header' via PSI target", targets.isEmpty())
    }
}
