<?php

declare(strict_types=1);

namespace Twig\Profiler {
    final class Profile
    {
        /** @param list<Profile> $profiles */
        public function __construct(
            private string $template,
            private string $type,
            private string $name,
            private array $starts,
            private array $ends,
            private array $profiles = [],
        ) {
        }

        /** @return array{string, string, string, array<string, float>, array<string, float>, list<Profile>} */
        public function __serialize(): array
        {
            return [$this->template, $this->name, $this->type, $this->starts, $this->ends, $this->profiles];
        }
    }
}

namespace Symfony\Bridge\Twig\DataCollector {
    final class TwigDataCollector
    {
        /** @param array<string, mixed> $data */
        public function __construct(protected array $data)
        {
        }
    }
}

namespace {
    use Symfony\Bridge\Twig\DataCollector\TwigDataCollector;
    use Twig\Profiler\Profile;

    /** @param list<Profile> $children */
    function profile(
        string $template,
        string $type,
        string $name,
        float $start,
        float $end,
        array $children = [],
    ): Profile {
        return new Profile($template, $type, $name, ['wt' => $start], ['wt' => $end], $children);
    }

    // Generates the Twig profile used by SymfonyProfilerTwigConsumerTest.
    // Names, paths, and values are generic and contain no application-specific data.
    $base = profile(
        'base.html.twig',
        'template',
        'base.html.twig',
        100.002,
        100.008,
        [
            profile('catalog/detail.html.twig', 'block', 'title', 100.0021, 100.0023),
            profile('partials/navigation.html.twig', 'template', 'partials/navigation.html.twig', 100.003, 100.0045),
            profile('components/card.html.twig', 'template', 'components/card.html.twig', 100.0045, 100.0056),
            profile('components/card.html.twig', 'template', 'components/card.html.twig', 100.0056, 100.0065),
            profile('emails/banner.html.twig', 'template', 'emails/banner.html.twig', 100.0065, 100.0077),
        ],
    );
    $entry = profile(
        'catalog/detail.html.twig',
        'template',
        'catalog/detail.html.twig',
        100.0,
        100.012,
        [
            profile('components/price.html.twig', 'macro', 'format_price', 100.0005, 100.001),
            $base,
        ],
    );
    $toolbar = profile(
        '@WebProfiler/Profiler/toolbar_js.html.twig',
        'template',
        '@WebProfiler/Profiler/toolbar_js.html.twig',
        100.012,
        100.014,
        [
            profile(
                '@WebProfiler/Profiler/toolbar.html.twig',
                'template',
                '@WebProfiler/Profiler/toolbar.html.twig',
                100.0122,
                100.0137,
            ),
        ],
    );
    $root = profile('main', 'ROOT', 'main', 100.0, 100.014, [$entry, $toolbar]);

    $collector = new TwigDataCollector([
        'profile' => serialize($root),
        'template_paths' => [
            'catalog/detail.html.twig' => 'templates/catalog/detail.html.twig',
            'base.html.twig' => 'templates/base.html.twig',
            'partials/navigation.html.twig' => 'templates/partials/navigation.html.twig',
            'components/card.html.twig' => 'templates/components/card.html.twig',
            'emails/banner.html.twig' => 'templates/emails/banner.html.twig',
            '@WebProfiler/Profiler/toolbar_js.html.twig' => 'vendor/symfony/web-profiler-bundle/Resources/views/Profiler/toolbar_js.html.twig',
            '@WebProfiler/Profiler/toolbar.html.twig' => 'vendor/symfony/web-profiler-bundle/Resources/views/Profiler/toolbar.html.twig',
        ],
    ]);

    $requestProfile = [
        'token' => 'c0ffee',
        'parent' => null,
        'children' => [],
        'data' => ['twig' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'GET',
        'url' => 'http://example.test/catalog/42',
        'time' => 1_723_557_600,
        'status_code' => 200,
    ];

    $serialized = serialize($requestProfile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['twig'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    if ($compressed === false || file_put_contents($directory . '/symfony-profiler-twig.gz', $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated neutral Symfony twig profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
