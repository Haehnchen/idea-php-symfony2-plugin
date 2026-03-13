package fr.adrienbrault.idea.symfony2plugin.tests.twig.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil

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
}
