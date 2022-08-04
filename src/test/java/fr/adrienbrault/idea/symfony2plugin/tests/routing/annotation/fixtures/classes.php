<?php

namespace Symfony\Component\Routing\Annotation
{
    /**
     * @Annotation
     */
    class Route
    {
        public function __construct(
            $data = [],
            $path = null,
            string $name = null,
            array $requirements = [],
            array $options = [],
            array $defaults = [],
            string $host = null,
            array $methods = [],
            array $schemes = [],
            string $condition = null,
            int $priority = null,
            string $locale = null,
            string $format = null,
            bool $utf8 = null,
            bool $stateless = null
        ) {
        }
    }
}