package fr.adrienbrault.idea.symfonyplugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorProviderDict {
    public static class TranslationDomain {
        @NotNull
        private final String domain;
        private final boolean weak;

        public TranslationDomain(@NotNull String domain) {
            this.domain = domain;
            this.weak = false;
        }

        public TranslationDomain(@NotNull String domain, boolean weak) {
            this.domain = domain;
            this.weak = weak;
        }

        @NotNull
        public String getDomain() {
            return domain;
        }

        public boolean isWeak() {
            return weak;
        }
    }

    public static class TranslationKey {
        @NotNull
        private final String domain;
        private final boolean weak;

        public TranslationKey(@NotNull String domain) {
            this.domain = domain;
            this.weak = false;
        }

        public TranslationKey(@NotNull String domain, boolean weak) {
            this.domain = domain;
            this.weak = weak;
        }

        @NotNull
        public String getDomain() {
            return domain;
        }

        public boolean isWeak() {
            return weak;
        }
    }
}
