<?php
namespace
{
    interface ArrayAccess {}
    interface Iterator {}
    class ArrayIterator implements Iterator, ArrayAccess {}
}

namespace Foo
{
    class Bar
    {
        const FOO = null;
        private $private;
        public $public;
        /**
         * @deprecated
         */
        public $deprecatedProperty;
        #[\Deprecated(message: "use safe_replacement() instead", since: "1.5")]
        public $deprecatedAttributeProperty;

        public function getFoo() {}
        public function hasHassers() {}
        public function isCar() {}
        protected function getApple() {}

        public function getNext() {
            return new Bar();
        }

        public function getArray() {
            return ['foo' => 'foobar'];
        }

        /**
         * @deprecated
         */
        public function getDeprecated() {
        }

        #[\Deprecated(message: "use safe_replacement() instead", since: "1.5")]
        public function getAttributeDeprecated() {
        }
    }

    abstract class BarArrayAccess implements \ArrayAccess
    {
    }
    abstract class BarIterator implements \Iterator
    {
    }

    abstract class BarArrayIterator extends \ArrayIterator
    {
    }
}

namespace Sensio\Bundle\FrameworkExtraBundle\Configuration
{
    class Template
    {
    }
}

namespace Symfony\Component\Asset
{
    class Packages
    {
        public function getVersion(string $path, string $packageName = null)
        {
        }

        public function getUrl(string $path, string $packageName = null)
        {
        }
    }

    interface PackageInterface
    {
        public function getVersion(string $path): string;
        public function getUrl(string $path): string;
    }

    class Package implements PackageInterface
    {
    }
}
