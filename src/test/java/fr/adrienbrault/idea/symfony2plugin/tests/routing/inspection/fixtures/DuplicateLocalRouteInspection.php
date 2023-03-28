<?php

namespace Symfony\Component\Routing\Annotation
{
    /**
     * Annotation class for @Route().
     *
     * @Annotation
     * @NamedArgumentConstructor
     * @Target({"CLASS", "METHOD"})
     *
     * @author Fabien Potencier <fabien@symfony.com>
     * @author Alexander M. Turek <me@derrabus.de>
     */
    #[\Attribute(\Attribute::IS_REPEATABLE | \Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD)]
    class Route
    {
    }
}
