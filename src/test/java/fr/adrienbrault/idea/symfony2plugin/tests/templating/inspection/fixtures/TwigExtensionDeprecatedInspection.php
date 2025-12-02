<?php

namespace Twig\TokenParser {
    interface TokenParserInterface {}

    /**
     * @deprecated Foobar deprecated message
     */
    class SpacelessTokenParser implements TokenParserInterface
    {
        public function getTag()
        {
            return 'spaceless';
        }
    }

    class SandboxTokenParser implements TokenParserInterface
    {
        public function getTag()
        {
            return 'sandbox';
        }

        public function parse()
        {
            trigger_deprecation('twig/twig', '3.9', 'The "sandbox" tag is deprecated.');
            // parse logic here
        }
    }
}

namespace {
    function trigger_deprecation(string $package, string $version, string $message, mixed ...$args): void {}

    interface Twig_ExtensionInterface {
        public function getFilters();
        public function getFunctions();
    }

    class DeprecatedCallableInfo {
        public function triggerDeprecation(string $path, int $line): void {}
    }
}

namespace Twig {
    class TwigFilter {}
    class TwigFunction {}

    class Extensions implements \Twig_ExtensionInterface
    {
        public function getFilters()
        {
            return [
                new TwigFilter('trans', [$this, 'foobar']),
                new TwigFilter('trans_2', [$this, 'foobar']),
                new TwigFilter('spaceless_deprecation_info', [self::class, 'spaceless'], ['is_safe' => ['html'], 'deprecation_info' => new \Twig\DeprecatedCallableInfo('twig/twig', '3.12')]),
                new TwigFilter('spaceless_deprecation_deprecated', [self::class, 'spaceless'], ['is_safe' => ['html'], 'deprecated' => 12.12]),
                new TwigFilter('filter_with_trigger_deprecation', [self::class, 'filterWithTriggerDeprecation']),
            ];
        }

        public function getFunctions()
        {
            return [
                new TwigFunction('max', 'max'),
                new TwigFunction('deprecated_function_info', [self::class, 'deprecatedFunctionInfo'], ['deprecation_info' => new \Twig\DeprecatedCallableInfo('twig/twig', '3.12')]),
                new TwigFunction('deprecated_function', [self::class, 'deprecatedFunction'], ['deprecated' => 12.12]),
                new TwigFunction('function_with_trigger_deprecation', [self::class, 'functionWithTriggerDeprecation']),
            ];
        }

        public function foobar() {}
        public static function spaceless() {}
        public static function deprecatedFunctionInfo() { return 'test'; }
        public static function deprecatedFunction() { return 'test'; }

        public static function filterWithTriggerDeprecation(string $value): string {
            $dep = new \DeprecatedCallableInfo();
            $dep->triggerDeprecation('path', 1);
            return $value;
        }

        public static function functionWithTriggerDeprecation(): string {
            $dep = new \DeprecatedCallableInfo();
            $dep->triggerDeprecation('path', 1);
            return 'test';
        }
    }
}
