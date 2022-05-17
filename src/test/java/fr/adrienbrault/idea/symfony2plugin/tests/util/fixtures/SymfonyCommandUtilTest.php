<?php

namespace Symfony\Component\Console\Command
{
    class Command
    {
        function setName($i) {}
    }
}

namespace Foo
{
    use Symfony\Component\Console\Attribute\AsCommand;
    use Symfony\Component\Console\Command\Command;

    class FooCommand extends Command
    {
        public function configure()
        {
            $this->setName('foo');
        }
    }

    class PropertyCommand extends Command
    {
        private $name = 'property';

        public function configure()
        {
            $this->setName($this->name);
        }
    }

    class ConstCommand extends Command
    {
        const FOO = 'const';

        public function configure()
        {
            $this->setName(self::FOO);
        }
    }

    #[AsCommand('app:create-user-1')]
    class FoobarCommand1  extends Command {}

    #[AsCommand(name: 'app:create-user-2')]
    class FoobarCommand2  extends Command {}

    class FoobarCommand3 extends Command
    {
        protected static $defaultName = 'app:create-user-3';
    }
}