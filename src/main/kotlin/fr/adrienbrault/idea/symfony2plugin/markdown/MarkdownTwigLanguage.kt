package fr.adrienbrault.idea.symfony2plugin.markdown

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language
import com.jetbrains.twig.TwigLanguage

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object MarkdownTwigLanguage : Language(TwigLanguage.INSTANCE, "SymfonyMarkdownTwig"), InjectableLanguage
