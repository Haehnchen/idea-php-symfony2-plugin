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
