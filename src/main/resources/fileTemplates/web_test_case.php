<?php

declare(strict_types=1);

namespace {{ namespace }};

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class {{ class }} extends WebTestCase
{
    public function testSomething(): void
    {
        // This calls KernelTestCase::bootKernel(), and creates a
        // "client" that is acting as the browser
        $client = static::createClient();

        // Request a specific page
        $crawler = $client->request('GET', '/');

        // Validate a successful response and some content
        $this->assertResponseIsSuccessful();
        $this->assertSelectorTextContains('h1', 'Hello World');

        $this->assertGreaterThan(0, $crawler->filter('html:contains("Hello World")')->count());
    }
}
