<?php

namespace
{
    interface Twig_ExtensionInterface
    {
        public function getFilters();
        public function getFunctions();
    }
}

namespace Twig
{
    class TwigFilter
    {
        public function __construct(string $name, $callable = null, array $options = [])
        {
        }
    }

    class TwigFunction
    {
        public function __construct(string $name, $callable = null, array $options = [])
        {
        }
    }

    class Environment
    {
    }
}

namespace Twig\Extension
{
    interface ExtensionInterface
    {
        public function getFilters();
        public function getFunctions();
    }

    abstract class AbstractExtension implements ExtensionInterface
    {
        public function getFilters()
        {
            return [];
        }

        public function getFunctions()
        {
            return [];
        }
    }
}

namespace App\Twig
{
    use Twig\Extension\AbstractExtension;
    use Twig\TwigFilter;
    use Twig\TwigFunction;

    /**
     * Test Twig extension with filters and functions for named argument testing
     */
    class AppExtension extends AbstractExtension implements \Twig_ExtensionInterface
    {
        public function getFilters()
        {
            return [
                new TwigFilter('date', [$this, 'dateFilter']),
                new TwigFilter('convert_encoding', [$this, 'convertEncodingFilter']),
            ];
        }

        public function getFunctions()
        {
            return [
                new TwigFunction('range', [$this, 'rangeFunction']),
            ];
        }

        /**
         * Date filter with format and timezone parameters
         * Environment parameter should be filtered out from completion
         *
         * @param \Twig\Environment $env Twig environment (should be hidden)
         * @param mixed $value The value to format (piped value)
         * @param string|null $format Date format
         * @param string|null $timezone Timezone string
         * @return string
         */
        public function dateFilter(\Twig\Environment $env, $value, $format = null, $timezone = null)
        {
            return '';
        }

        /**
         * Convert encoding filter
         *
         * @param string $value The value to convert (piped value)
         * @param string $from Source encoding
         * @param string $to Target encoding
         * @return string
         */
        public function convertEncodingFilter($value, $from, $to)
        {
            return '';
        }

        /**
         * Range function with named parameters
         *
         * @param int $low Lower bound
         * @param int $high Upper bound
         * @param int $step Step increment
         * @return array
         */
        public function rangeFunction($low, $high, $step = 1)
        {
            return [];
        }
    }
}
