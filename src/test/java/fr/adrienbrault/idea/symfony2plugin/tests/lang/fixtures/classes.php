<?php

namespace Symfony\Component\DomCrawler {
    class Crawler
    {
        public function filter($str) {}
        public function children($str) {}
        public function filterXPath($str) {}
        public function evaluate($str) {}
    }
}

namespace Symfony\Component\CssSelector {
    class CssSelectorConverter
    {
        public function toXPath($str) {}
    }
}

namespace Symfony\Component\HttpFoundation {
    class JsonResponse {
        public static function fromJsonString() {}
        public function setJson() {}
    }
}

namespace Symfony\Component\DependencyInjection\Loader\Configurator {
    function expr(string $expression) {}
}

namespace Symfony\Component\ExpressionLanguage {
    class ExpressionLanguage
    {
        public function compile($expression, array $names = []) {}
        public function evaluate($expression, array $values = []) {}
        public function parse($expression, array $names = []) {}
    }

    class Expression
    {
        public function __construct(string $expression) {}
    }
}

namespace Symfony\Component\Routing\Annotation {
    /**
     * @Annotation
     * @Target({"CLASS", "METHOD"})
     */
    #[\Attribute(\Attribute::IS_REPEATABLE | \Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD)]
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
        ) {}
    }
}

namespace Symfony\Component\Routing\Loader\Configurator\Traits {
    trait RouteTrait
    {
        public function condition(string $condition): {}
    }
}

namespace Symfony\Component\Routing\Loader\Configurator {
    class RoutingConfigurator
    {
        use \Symfony\Component\Routing\Loader\Configurator\Traits\RouteTrait;
    }
}

namespace Symfony\Component\Validator\Constraints {
    /**
     * @Annotation
     * @Target({"CLASS", "PROPERTY", "METHOD", "ANNOTATION"})
     */
    #[\Attribute(\Attribute::TARGET_PROPERTY | \Attribute::TARGET_METHOD | \Attribute::TARGET_CLASS | \Attribute::IS_REPEATABLE)]
    class Expression
    {
        public function __construct(
            $expression,
            string $message = null,
            array $values = null,
            array $groups = null,
            $payload = null,
            array $options = []
        ) {}
    }
}

namespace Sensio\Bundle\FrameworkExtraBundle\Configuration {
    /**
     * @Annotation
     */
     #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD)]
    class Cache
    {
        public function __construct(
            array $values = [],
            string $expires = null,
            $maxage = null,
            $smaxage = null,
            bool $public = null,
            bool $mustRevalidate = null,
            array $vary = null,
            string $lastModified = null,
            string $etag = null,
            $maxstale = null,
            $staleWhileRevalidate = null,
            $staleIfError = null
        ) {}
    }

    /**
     * @Annotation
     */
     #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD)]
    class Entity {
        public function __construct(
            $data = [],
            string $expr = null,
            string $class = null,
            array $options = [],
            bool $isOptional = false,
            string $converter = null
        ) {}
    }

    /**
     * @Annotation
     */
     #[\Attribute(\Attribute::TARGET_CLASS | \Attribute::TARGET_METHOD)]
    class Security
    {
        public function __construct(
            $data = [],
            string $message = null,
            ?int $statusCode = null
        ) {}
    }

    /**
     * @Annotation
     */
    class Route extends \Symfony\Component\Routing\Annotation\Route {}
}

namespace Doctrine\ORM {
    class EntityManager {
        public function createQuery() {}
    }
    class Query {
        public function setDQL() {}
    }
}

namespace Foo {
    class Bar
    {
    }
}
