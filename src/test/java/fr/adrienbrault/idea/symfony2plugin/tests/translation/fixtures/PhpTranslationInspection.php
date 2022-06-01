<?php

namespace Symfony\Component\Translation
{
    interface TranslatorInterface
    {
        public function transChoice();
        public function trans();
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
