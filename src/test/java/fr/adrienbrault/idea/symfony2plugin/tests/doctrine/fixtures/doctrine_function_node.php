<?php

namespace Doctrine\ORM\Query\AST\Functions
{
    abstract class FunctionNode
    {
        public function getSql(SqlWalker $sqlWalker)
        {
        }
    }
}

namespace Doctrine\ORM\Query
{
    class Parser {
        private static $stringFunctions = [
            'substring' => Functions\SubstringFunction::class,
        ];

        /**
         * @readonly Maps BUILT-IN numeric function names to AST class names.
         * @psalm-var array<string, class-string<Functions\FunctionNode>>
         */
        private static $numericFunctions = [
            'length'    => Functions\LengthFunction::class,
            // Aggregate functions
            'min'       => Functions\MinFunction::class,
        ];

        /**
         * @readonly Maps BUILT-IN datetime function names to AST class names.
         * @psalm-var array<string, class-string<Functions\FunctionNode>>
         */
        private static $datetimeFunctions = [
            'current_time'      => Functions\CurrentTimeFunction::class,
        ];
    }
}
