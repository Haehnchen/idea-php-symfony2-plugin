package fr.adrienbrault.idea.symfony2plugin.tests.icon

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.LayeredIcon
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.icon.PhpClassFileIconProvider
import fr.adrienbrault.idea.symfony2plugin.icon.createDecoratedFileIcon
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import javax.swing.Icon

/**
 * @see PhpClassFileIconProvider
 */
class PhpClassFileIconProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testAsCommandAttributeShowsCommandBadge() {
        val commandFile = myFixture.addFileToProject(
            "src/Command/FooCommand.php",
            """
            <?php

            namespace App\Command;

            use Symfony\Component\Console\Attribute\AsCommand;

            #[AsCommand(name: 'app:foo')]
            class FooCommand
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for AsCommand class", getIconFromProvider(commandFile))
    }

    fun testDoctrineEntityAttributeShowsDoctrineBadge() {
        val entityFile = myFixture.addFileToProject(
            "src/Entity/Product.php",
            """
            <?php

            namespace App\Entity;

            use Doctrine\ORM\Mapping as ORM;

            #[ORM\Entity]
            class Product
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Doctrine entity class", getIconFromProvider(entityFile))
    }

    fun testDoctrineEntityAnnotationShowsDoctrineBadge() {
        val entityFile = myFixture.addFileToProject(
            "src/Entity/Category.php",
            """
            <?php

            namespace App\Entity;

            use Doctrine\ORM\Mapping as ORM;

            /**
             * @ORM\Entity
             */
            class Category
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Doctrine entity annotation", getIconFromProvider(entityFile))
    }

    fun testDoctrineRepositoryShowsRepositoryBadge() {
        myFixture.addFileToProject(
            "vendor/doctrine/orm/EntityRepository.php",
            """
            <?php

            namespace Doctrine\ORM;

            class EntityRepository
            {
            }
            """.trimIndent()
        )
        val repositoryFile = myFixture.addFileToProject(
            "src/Repository/ProductRepository.php",
            """
            <?php

            namespace App\Repository;

            class ProductRepository extends \Doctrine\ORM\EntityRepository
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Doctrine repository class", getIconFromProvider(repositoryFile))
    }

    fun testSymfonyFormAbstractTypeShowsFormBadge() {
        myFixture.addFileToProject(
            "vendor/symfony/form/AbstractType.php",
            """
            <?php

            namespace Symfony\Component\Form;

            abstract class AbstractType
            {
            }
            """.trimIndent()
        )
        val formTypeFile = myFixture.addFileToProject(
            "src/Form/ProductType.php",
            """
            <?php

            namespace App\Form;

            class ProductType extends \Symfony\Component\Form\AbstractType
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Symfony form type class", getIconFromProvider(formTypeFile))
    }

    fun testRouteAttributeShowsControllerBadge() {
        val controllerFile = myFixture.addFileToProject(
            "src/Controller/DashboardController.php",
            """
            <?php

            namespace App\Controller;

            use Symfony\Component\Routing\Attribute\Route;

            class DashboardController
            {
                #[Route('/dashboard', name: 'app_dashboard')]
                public function index()
                {
                }
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Symfony controller route attribute", getIconFromProvider(controllerFile))
    }

    fun testSymfonyAbstractControllerShowsControllerBadge() {
        myFixture.addFileToProject(
            "vendor/symfony/framework-bundle/Controller/AbstractController.php",
            """
            <?php

            namespace Symfony\Bundle\FrameworkBundle\Controller;

            abstract class AbstractController
            {
            }
            """.trimIndent()
        )
        val controllerFile = myFixture.addFileToProject(
            "src/Controller/AdminController.php",
            """
            <?php

            namespace App\Controller;

            use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;

            class AdminController extends AbstractController
            {
            }
            """.trimIndent()
        )

        assertNotNull("Icon should not be null for Symfony controller inheritance", getIconFromProvider(controllerFile))
    }

    fun testBadgeIconsAreLayered() {
        val commandIcon: Icon = createDecoratedFileIcon(Symfony2Icons.SYMFONY, Symfony2Icons.SYMFONY_COMMAND_FILE)
        val entityIcon: Icon = createDecoratedFileIcon(Symfony2Icons.SYMFONY, Symfony2Icons.DOCTRINE_ENTITY_FILE)
        val repositoryIcon: Icon = createDecoratedFileIcon(Symfony2Icons.SYMFONY, Symfony2Icons.DOCTRINE_REPOSITORY_FILE)
        val formTypeIcon: Icon = createDecoratedFileIcon(Symfony2Icons.SYMFONY, Symfony2Icons.FORM_TYPE_FILE)
        val controllerIcon: Icon = createDecoratedFileIcon(Symfony2Icons.SYMFONY, Symfony2Icons.CONTROLLER_FILE)

        assertTrue("Command badge icon should be layered", commandIcon is LayeredIcon)
        assertTrue("Entity badge icon should be layered", entityIcon is LayeredIcon)
        assertTrue("Repository badge icon should be layered", repositoryIcon is LayeredIcon)
        assertTrue("Form type badge icon should be layered", formTypeIcon is LayeredIcon)
        assertTrue("Controller badge icon should be layered", controllerIcon is LayeredIcon)
        assertTrue("Command badge should be loaded", Symfony2Icons.SYMFONY_COMMAND_FILE.iconWidth > 0)
        assertTrue("Entity badge should be loaded", Symfony2Icons.DOCTRINE_ENTITY_FILE.iconWidth > 0)
        assertTrue("Repository badge should be loaded", Symfony2Icons.DOCTRINE_REPOSITORY_FILE.iconWidth > 0)
        assertTrue("Form type badge should be loaded", Symfony2Icons.FORM_TYPE_FILE.iconWidth > 0)
        assertTrue("Controller badge should be loaded", Symfony2Icons.CONTROLLER_FILE.iconWidth > 0)
    }

    fun testRegularPhpClassFallsBackToPhpProvider() {
        val regularFile = myFixture.addFileToProject(
            "src/Service/ProductDto.php",
            """
            <?php

            namespace App\Service;

            class ProductDto
            {
            }
            """.trimIndent()
        )

        assertNull("Provider should not handle regular PHP classes", getIconFromProvider(regularFile))
    }

    fun testMultipleClassesFallBackToPhpProvider() {
        val multipleClassesFile = myFixture.addFileToProject(
            "src/Command/FooCommand.php",
            """
            <?php

            namespace App\Command;

            use Symfony\Component\Console\Attribute\AsCommand;

            #[AsCommand(name: 'app:foo')]
            class FooCommand
            {
            }

            class Helper
            {
            }
            """.trimIndent()
        )

        assertNull("Provider should not handle PHP files with multiple classes", getIconFromProvider(multipleClassesFile))
    }

    private fun getIconFromProvider(psiFile: PsiFile): Icon? {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return PhpClassFileIconProvider().getIcon(psiFile.virtualFile!!, 0, project)
    }
}
