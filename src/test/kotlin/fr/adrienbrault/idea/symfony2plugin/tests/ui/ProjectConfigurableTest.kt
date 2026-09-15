package fr.adrienbrault.idea.symfony2plugin.tests.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.SettingsForm
import fr.adrienbrault.idea.symfony2plugin.profiler.ui.ProfilerSettingsDialog
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.ui.ContainerSettingsForm
import fr.adrienbrault.idea.symfony2plugin.ui.MethodParameterReferenceSettingsForm
import fr.adrienbrault.idea.symfony2plugin.ui.MethodSignatureTypeSettingsForm
import fr.adrienbrault.idea.symfony2plugin.ui.RoutingSettingsForm
import fr.adrienbrault.idea.symfony2plugin.ui.TwigSettingsForm

class ProjectConfigurableTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testSettingsFormLifecycle() {
        assertConfigurableLifecycle(SettingsForm(project))
    }

    fun testSettingsFormKeepsStaticShowApi() {
        assertNotNull(SettingsForm::class.java.getMethod("show", Project::class.java))
    }

    fun testContainerSettingsFormLifecycle() {
        assertConfigurableLifecycle(ContainerSettingsForm(project))
    }

    fun testMethodParameterReferenceSettingsFormLifecycle() {
        assertConfigurableLifecycle(MethodParameterReferenceSettingsForm(project))
    }

    fun testMethodSignatureTypeSettingsFormLifecycle() {
        assertConfigurableLifecycle(MethodSignatureTypeSettingsForm(project))
    }

    fun testRoutingSettingsFormLifecycle() {
        assertConfigurableLifecycle(RoutingSettingsForm(project))
    }

    fun testProfilerSettingsDialogLifecycle() {
        val configurable = ProfilerSettingsDialog(project)

        assertNotNull(configurable.createComponent())
        configurable.reset()
        configurable.apply()
        assertFalse(configurable.isModified)
        configurable.disposeUIResources()
    }

    fun testTwigSettingsFormLifecycle() {
        val configurable = TwigSettingsForm(project)

        assertNotNull(configurable.createComponent())
        configurable.reset()
        configurable.disposeUIResources()
    }

    private fun assertConfigurableLifecycle(configurable: Configurable) {
        assertNotNull(configurable.createComponent())

        configurable.reset()
        assertFalse(configurable.isModified)

        configurable.apply()
        assertFalse(configurable.isModified)

        configurable.disposeUIResources()
    }
}
