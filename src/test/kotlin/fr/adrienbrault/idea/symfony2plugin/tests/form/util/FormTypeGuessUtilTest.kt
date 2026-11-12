package fr.adrienbrault.idea.symfony2plugin.tests.form.util

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.MethodReference
import fr.adrienbrault.idea.symfony2plugin.form.util.getFormType
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormTypeGuessUtilTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("FormTypeGuessUtilTest.php")
    }

    override fun getTestDataPath() = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/util/fixtures"

    fun testNativeTypes() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("capacity"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\NumberType", guessedTypeByProperty("price"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CheckboxType", guessedTypeByProperty("isPublic"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("startsAt"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("immutableStartsAt"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("dateTimeInterface"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("datePoint"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateIntervalType", guessedTypeByProperty("duration"))
    }

    fun testNullableAndAliasedTypes() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("nullableCapacity"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("nullableStartsAt"))
    }

    fun testPromotedConstructorProperty() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("promotedCapacity"))
    }

    fun testDoctrineDateColumns() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateType", guessedTypeByProperty("day"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateType", guessedTypeByProperty("immutableDay"))
    }

    fun testDoctrineTimeColumns() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TimeType", guessedTypeByProperty("startTime"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TimeType", guessedTypeByProperty("immutableTime"))
    }

    fun testDoctrineScalarColumns() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\NumberType", guessedTypeByProperty("amount"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("externalId"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CheckboxType", guessedTypeByProperty("enabled"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", guessedTypeByProperty("title"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextareaType", guessedTypeByProperty("description"))
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CollectionType", guessedTypeByProperty("tags"))
    }

    fun testDoctrineAssociations() {
        assertEquals("\\Symfony\\Bridge\\Doctrine\\Form\\Type\\EntityType", guessedTypeByProperty("parent"))
        assertEquals("\\Symfony\\Bridge\\Doctrine\\Form\\Type\\EntityType", guessedTypeByProperty("children"))
    }

    fun testCustomDoctrineColumnUsesNativeType() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("customCapacity"))
    }

    fun testOtherDoctrineColumnsUseTextType() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", guessedTypeByProperty("settings"))
    }

    fun testExplicitFormTypeOverridesDoctrineColumn() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType", guessedTypeByProperty("explicitDate"))
    }

    fun testBuilderPropertyPathUsesDoctrineColumn() {
        val type = guessedBuilderType("createDay")

        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateType", type)
    }

    fun testExplicitTypeTakesPrecedence() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\EmailType", guessedTypeByProperty("explicitType"))
    }

    fun testExplicitNullAllowsGuessing() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("guessedType"))
    }

    fun testPropertyPathOverridesFieldName() {
        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", guessedTypeByProperty("capacityLabel"))
    }

    fun testBuilderWithNamedChildAndOmittedType() {
        val type = guessedBuilderType("addCapacity")

        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", type)
    }

    fun testCreateWithNamedArgumentsAndPropertyPath() {
        val type = guessedBuilderType("createMinutes")

        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType", type)
    }

    fun testBuilderExplicitTypeTakesPrecedence() {
        val type = guessedBuilderType("addExplicitType")

        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", type)
    }

    private fun guessedBuilderType(methodName: String): String? {
        val formType = PhpElementsUtil.getClass(project, "App\\Form\\EventType")!!
        val method = formType.findMethodByName(methodName)!!
        val call = PsiTreeUtil.findChildOfType(method, MethodReference::class.java)!!

        return getFormType(call)
    }

    private fun guessedTypeByProperty(propertyName: String): String? {
        val dataClass = PhpElementsUtil.getClass(project, "App\\Form\\EventData")!!
        val field = dataClass.findFieldByName(propertyName, false)!!
        val attribute = field.getAttributes(FORM_FIELD_ATTRIBUTE).single()

        return getFormType(attribute)
    }
}
