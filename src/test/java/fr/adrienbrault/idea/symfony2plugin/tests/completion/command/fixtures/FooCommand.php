<?php

namespace Symfony\Component\Console\Command {

    class Command {
        protected function configure() {}
        function addOption()  { return $this; }
        function addArgument() { return $this; }
        function setDefinition() { return $this; }
    };

}

namespace Symfony\Component\Console\Input {

    interface InputInterface {
        public function getArgument();
        public function hasArgument();
        public function hasOption();
        public function getOption();
    }

    class InputArgument {}
    class InputOption {}

}

namespace Foo {

    use Symfony\Component\Console\Command\Command;
    use Symfony\Component\Console\Input\InputArgument;
    use Symfony\Component\Console\Input\InputInterface;
    use Symfony\Component\Console\Input\InputOption;

    class HelpCommand extends Command
    {
        protected function configure()
        {
            $this->addArgument('arg1', null, 'desc', 'default')
                ->addArgument('arg2')
                ->addArgument('arg3', null, 'desc')
                ->addOption('opt1', null, null, 'desc', 'default')
                ->addOption('opt2')
                ->addOption('opt3', null, null, 'desc');

            $this->addArgument('arg4', null, 'desc');
            $this->addOption('opt4', null, null, 'desc');

            $this->setDefinition(array(
                new InputArgument('argDef', null, 'desc', 'default'),
                new InputOption('optDef', null, null, 'desc'),
            ));

            $this->setDefinition(new InputOption('optSingleDef', null, null, 'desc'));

        }

        protected function execute(InputInterface $input)
        {
            $input->getArgument("<getArgument>");
            $input->hasArgument("<hasArgument>");

            $input->getOption("<getOption>");
            $input->hasOption("<hasOption>");
        }

    }
}


