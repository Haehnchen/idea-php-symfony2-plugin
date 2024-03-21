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


    if (!\function_exists(t::class)) {
        /**
         * @author Nate Wiebe <nate@northern.co>
         */
        function t(string $message, array $parameters = [], string $domain = null): TranslatableMessage
        {
            return new TranslatableMessage($message, $parameters, $domain);
        }
    }
}
