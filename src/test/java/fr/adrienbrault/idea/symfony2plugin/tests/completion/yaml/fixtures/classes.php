<?php


namespace Symfony\Contracts\Translation
{
    interface TranslatorInterface
    {
        public function trans(string $id, array $parameters = [], string $domain = null, string $locale = null): string;
    }
}