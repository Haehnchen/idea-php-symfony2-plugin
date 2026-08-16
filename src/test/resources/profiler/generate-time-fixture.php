<?php

declare(strict_types=1);

namespace Symfony\Component\Stopwatch {
    final class StopwatchPeriod
    {
        public function __construct(
            private float $start,
            private float $end,
            private int $memory,
        ) {
        }
    }

    final class StopwatchEvent
    {
        /** @param list<StopwatchPeriod> $periods */
        public function __construct(
            private array $periods,
            private float $origin,
            private string $category,
            private bool $morePrecision = true,
            private array $started = [],
            private string $name = 'default',
        ) {
        }
    }
}

namespace Symfony\Component\HttpKernel\DataCollector {
    final class TimeDataCollector
    {
        /** @param array<string, mixed> $data */
        public function __construct(protected array $data)
        {
        }
    }
}

namespace {
    use Symfony\Component\HttpKernel\DataCollector\TimeDataCollector;
    use Symfony\Component\Stopwatch\StopwatchEvent;
    use Symfony\Component\Stopwatch\StopwatchPeriod;

    // Generates the timing profile used by SymfonyProfilerTimeConsumerTest.
    // Names and values are deliberately generic and contain no application-specific data.
    $startTime = 1_723_557_600_000.0;
    $eventOrigin = $startTime + 12.34;
    $events = [
        '__section__' => new StopwatchEvent(
            [new StopwatchPeriod(0.0, 120.0, 8 * 1024 * 1024)],
            $eventOrigin,
            'section',
            name: '__section__',
        ),
        'kernel.request' => new StopwatchEvent(
            [new StopwatchPeriod(0.0, 8.75, 4 * 1024 * 1024)],
            $eventOrigin,
            'event_listener',
            name: 'kernel.request',
        ),
        'controller' => new StopwatchEvent(
            [
                new StopwatchPeriod(10.0, 35.0, 6 * 1024 * 1024),
                new StopwatchPeriod(50.0, 95.0, 8 * 1024 * 1024),
            ],
            $eventOrigin,
            'section',
            name: 'controller',
        ),
        'view' => new StopwatchEvent(
            [new StopwatchPeriod(96.0, 120.0, 7 * 1024 * 1024)],
            $eventOrigin,
            'template',
            name: 'view',
        ),
        'response.listener' => new StopwatchEvent(
            [new StopwatchPeriod(80.0, 92.5, 5 * 1024 * 1024)],
            $eventOrigin,
            'event_listener',
            name: 'response.listener',
        ),
    ];

    $collector = new TimeDataCollector([
        'start_time' => $startTime,
        'events' => $events,
        'stopwatch_installed' => true,
    ]);

    $profile = [
        'token' => 'fedcba',
        'parent' => null,
        'children' => [],
        'data' => ['time' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'GET',
        'url' => 'http://example.test/performance',
        'time' => 1_723_557_600,
        'status_code' => 200,
    ];

    $serialized = serialize($profile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['time'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    if ($compressed === false || file_put_contents($directory . '/symfony-profiler-time.gz', $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated neutral Symfony time profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
