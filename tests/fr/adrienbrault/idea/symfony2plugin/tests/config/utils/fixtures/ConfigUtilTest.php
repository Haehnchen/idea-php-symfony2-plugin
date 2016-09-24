<?php

namespace Symfony\Component\Config\Definition
{
    interface ConfigurationInterface
    {
    }
}

namespace Symfony\Component\Config\Definition\Builder
{
    class TreeBuilder
    {
        public function root($name, $type = 'array', $builder = null)
        {
        }
    }
}

namespace Foo\Bar
{
    use Symfony\Component\Config\Definition\Builder\TreeBuilder;
    use Symfony\Component\Config\Definition\ConfigurationInterface;

    class MyConfiguration implements ConfigurationInterface
    {
        /**
         * {@inheritDoc}
         */
        public function getConfigTreeBuilder()
        {
            (new TreeBuilder())->root('foobar_root');
        }
    }

    class MyNextConfiguration implements ConfigurationInterface
    {
        /**
         * {@inheritDoc}
         */
        public function getConfigTreeBuilder()
        {
            if(true) {
                foreach([] as $_) {
                    (new TreeBuilder())->root('foobar_root');
                }
            }
        }
    }
}
