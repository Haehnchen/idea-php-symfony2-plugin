<?php

namespace App\Controller
{
    class BarController
    {
        /**
         * @deprecated
         */
        public function foobar() {}
    }

    /**
     * @deprecated
     */
    class CarController
    {
        public function foobar() {}
    }

    class DeprecatedAttributeController
    {
        /**
         * @deprecated This method is deprecated using PHPDoc
         */
        public function oldMethod() {}

        #[\Deprecated]
        public function newDeprecatedMethod() {}

        #[\Deprecated("This method is deprecated with a message")]
        public function newDeprecatedMethodWithMessage() {}

        public function notDeprecatedMethod() {}
    }

    #[\Deprecated]
    class DeprecatedClassController
    {
        public function someMethod() {}
    }

    #[\Deprecated("This class is deprecated with a message")]
    class DeprecatedClassWithMessageController
    {
        public function someMethod() {}
    }
}
