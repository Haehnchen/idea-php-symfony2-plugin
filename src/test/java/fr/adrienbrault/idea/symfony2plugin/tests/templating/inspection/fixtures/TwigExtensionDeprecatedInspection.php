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
}
