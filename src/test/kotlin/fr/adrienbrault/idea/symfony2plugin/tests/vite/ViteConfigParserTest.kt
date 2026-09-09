package fr.adrienbrault.idea.symfony2plugin.tests.vite

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.vite.ViteConfigParser

class ViteConfigParserTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testOnlyExportedConfigInputPathsAreRead() {
        assertEntries("""
            const unused = { input: { unused: './unused.js' } };
            const unusedBuild = { build: { rollupOptions: { input: { unusedBuild: './unused.js' } } } };
            export default defineConfig({
                input: { app: './assets/app.js' },
                plugins: [plugin({ input: { plugin: './plugin.js' } })],
                custom: { input: { custom: './custom.js' } },
                rollupOptions: { input: { misplaced: './misplaced.js' } },
                build: { input: { misplacedBuild: './misplaced.js' } }
            });
        """, "app")
    }

    fun testNestedOptionsOverrideTopLevelDefault() {
        assertEntries("""
            export default {
                input: { fallback: './fallback.js' },
                build: {
                    rollupOptions: { input: { legacy: './legacy.js' } },
                    rolldownOptions: { input: { app: './app.js' } }
                }
            };
        """, "app")
    }

    fun testLocalConfigBuildAndEntryVariablesAndSpreads() {
        assertEntries("""
            const shared = { shared: './shared.js' };
            const entries = { ...shared, app: './app.js' };
            const options = { input: entries };
            const build = { rolldownOptions: options };
            const base = { build };
            const config = { ...base };
            export default defineConfig(config);
        """, "shared", "app")
    }

    fun testConditionalConfigReturnsExcludeNestedFunctionsAndPluginInputs() {
        assertEntries("""
            export default defineConfig(({ mode }) => {
                function unused() { return { input: { hidden: './hidden.js' } }; }
                if (mode === 'development') {
                    return { input: { dev: './dev.js' } };
                }
                return { build: { rollupOptions: { input: { prod: './prod.js' } } } };
            });
        """, "dev", "prod")
    }

    fun testShorthandConfigFunction() {
        assertEntries("""
            export default defineConfig(() => ({ input: { app: './app.js' } }));
        """, "app")
    }

    fun testLocalVariableShadowing() {
        assertEntries("""
            const entries = { wrong: './wrong.js' };
            export default defineConfig(() => {
                const entries = { app: './app.js' };
                return { input: entries };
            });
        """, "app")
    }

    fun testQualifiedAndImportedReferencesAreNotResolvedByName() {
        assertEntries("""
            import external from './external';
            const entries = { wrong: './wrong.js' };
            export default {
                input: external,
                build: { rolldownOptions: { input: other.entries } }
            };
        """)
    }

    fun testCyclicReferencesAndSpreadsTerminate() {
        assertEntries("""
            const first = second;
            const second = first;
            const entries = { ...entries, app: './app.js' };
            export default { ...first, input: entries };
        """, "app")
    }

    fun testArbitraryCallsAreNotTreatedAsConfigs() {
        assertEntries("""
            export default plugin({ input: { wrong: './wrong.js' } });
        """)
    }

    private fun assertEntries(config: String, vararg names: String) {
        val file = myFixture.addFileToProject("vite.config.ts", config.trimIndent())
        val entries = ViteConfigParser.parseEntries(file)
        assertEquals(names.toSet(), entries.map { it.name }.toSet())
        assertEquals(names.size, entries.size)
    }
}
