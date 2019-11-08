<?php

namespace Symfony\Component\Translation
{
    interface TranslatorInterface
    {
        public function trans($id, array $parameters = array(), $domain = null, $locale = null);
        public function transChoice($id, $number, array $parameters = array(), $domain = null, $locale = null);
    }
}