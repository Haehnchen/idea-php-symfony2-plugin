<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class {{ class }} extends WebTestCase
{
    public function testSomething(): void
    {
        // (1) boot the Symfony kernel
        self::bootKernel();

        // (2) use static::getContainer() to access the service container
        $container = static::getContainer();

        // (3) run some service & test the result
        $newsletterGenerator = $container->get(__CLASS__::class);
        $newsletter = $newsletterGenerator->generateMonthlyNews(/* ... */);

        $this->assertEquals('...', $newsletter->getContent());
    }
}
