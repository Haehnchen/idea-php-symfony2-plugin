<?php

namespace Symfony\Component\Translation
{
    interface TranslatorInterface
    {
        public function trans($id, array $parameters = array(), $domain = null, $locale = null);
        public function transChoice($id, $number, array $parameters = array(), $domain = null, $locale = null);
    }
}


namespace Symfony\Component\Translation
{
    interface TranslatableInterface
    {
        public function trans(TranslatorInterface $translator, string $locale = null): string;
    }

    class TranslatableMessage implements TranslatableInterface {}
}
