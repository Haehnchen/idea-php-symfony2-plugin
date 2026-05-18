<?php

namespace Twig\Extra\String {
    use Symfony\Component\String\UnicodeString;

    class StringExtension extends \Twig_Extension
    {
        public function getFilters()
        {
            return [
                new \Twig_SimpleFilter('u', [$this, 'createUnicodeString']),
            ];
        }

        public function getFunctions()
        {
            return [
                new \Twig_SimpleFunction('ustring', [$this, 'createUnicodeString']),
            ];
        }

        public function createUnicodeString(?string $text): UnicodeString
        {
            return new UnicodeString($text ?? '');
        }
    }
}

namespace Symfony\Component\String {
    class UnicodeString
    {
        public function truncate(int $length, string $ellipsis = '', bool $cut = true): static
        {
        }

        public function lower(): static
        {
        }
    }
}
