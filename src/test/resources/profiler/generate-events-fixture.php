<?php

declare(strict_types=1);

namespace Symfony\Component\VarDumper\Cloner {
    /** Minimal neutral fixture implementation of Symfony's serialized Stub shape. */
    final class Stub
    {
        public const TYPE_REF = 1;
        public const TYPE_STRING = 2;
        public const TYPE_ARRAY = 3;
        public const TYPE_OBJECT = 4;

        public int $type = self::TYPE_REF;
        public string|int|null $class = '';
        public mixed $value = null;
        public int $cut = 0;
        public int $handle = 0;
        public int $refCount = 0;
        public int $position = 0;
        public array $attr = [];

        public function __serialize(): array
        {
            $defaults = [
                'type' => self::TYPE_REF,
                'class' => '',
                'value' => null,
                'cut' => 0,
                'handle' => 0,
                'refCount' => 0,
                'position' => 0,
                'attr' => [],
            ];
            $data = [];
            foreach ($defaults as $name => $default) {
                if ($this->{$name} !== $default) {
                    $data[$name] = $this->{$name};
                }
            }

            return $data;
        }
    }

    /** Encodes arrays and objects into the position table consumed by Data::getValue(true). */
    final class Data
    {
        private array $data = [0 => []];
        private int $position = 0;
        private int|string $key = 0;
        private int $maxDepth = 20;
        private int $maxItemsPerDepth = -1;
        private int $useRefHandles = -1;
        private array $context = [];

        public function __construct(mixed $value)
        {
            $this->data[0][0] = $this->encode($value);
        }

        private function encode(mixed $value): mixed
        {
            if (is_array($value)) {
                if ([] === $value) {
                    return [];
                }

                $position = count($this->data);
                $this->data[$position] = [];
                foreach ($value as $key => $child) {
                    $this->data[$position][$key] = $this->encode($child);
                }

                return [(array_is_list($value) ? 2 : 1) => $position];
            }

            if (is_object($value)) {
                $position = count($this->data);
                $this->data[$position] = [];
                foreach ((array) $value as $key => $child) {
                    $this->data[$position][$key] = $this->encode($child);
                }

                $stub = new Stub();
                $stub->type = Stub::TYPE_OBJECT;
                $stub->class = $value::class;
                $stub->position = $position;

                return $stub;
            }

            return $value;
        }
    }
}

namespace Symfony\Component\HttpKernel\DataCollector {
    use Symfony\Component\VarDumper\Cloner\Data;

    final class EventDataCollector
    {
        public function __construct(protected Data $data)
        {
        }
    }
}

namespace {
    use Symfony\Component\HttpKernel\DataCollector\EventDataCollector;
    use Symfony\Component\VarDumper\Cloner\Data;

    /** @return array{event: string, priority: int, pretty: string, stub: string} */
    function listener(string $event, int $priority, string $pretty, string $signature): array
    {
        return [
            'event' => $event,
            'priority' => $priority,
            'pretty' => $pretty,
            'stub' => $signature,
        ];
    }

    /** @param array<string, mixed> $data */
    function writeFixture(string $filename, string $token, array $data): void
    {
        $collector = new EventDataCollector(new Data($data));
        $profile = [
            'token' => $token,
            'parent' => null,
            'children' => [],
            'data' => ['events' => $collector],
            'ip' => '127.0.0.1',
            'method' => 'GET',
            'url' => 'http://example.test/events',
            'time' => 1_723_557_600,
            'status_code' => 200,
        ];

        $serialized = serialize($profile);
        $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
        if (!is_array($roundTrip) || !isset($roundTrip['data']['events'])) {
            throw new RuntimeException('PHP rejected the generated profiler fixture');
        }

        $directory = __DIR__ . '/generated';
        if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
            throw new RuntimeException('Unable to create fixture directory');
        }

        $compressed = gzencode($serialized, 3);
        $target = $directory . '/' . $filename;
        if ($compressed === false || file_put_contents($target, $compressed) !== strlen($compressed)) {
            throw new RuntimeException('Unable to write profiler fixture');
        }
    }

    $mainListeners = [
        listener(
            'kernel.request',
            256,
            'Example\\EventListener\\RequestListener::validate',
            'Example\\EventListener\\RequestListener::validate(RequestEvent $event): void',
        ),
        listener(
            'kernel.request',
            32,
            'Example\\EventListener\\RouterListener::match',
            'Example\\EventListener\\RouterListener::match(RequestEvent $event): void',
        ),
        listener(
            'kernel.controller',
            0,
            'Example\\EventListener\\ControllerListener::prepare',
            'Example\\EventListener\\ControllerListener::prepare(ControllerEvent $event): void',
        ),
        listener(
            'kernel.request',
            -10,
            'Example\\EventListener\\LateRequestListener::inspect',
            'Example\\EventListener\\LateRequestListener::inspect(RequestEvent $event): void',
        ),
        listener(
            'kernel.response',
            -128,
            'Example\\EventListener\\ResponseListener::filter',
            "Example\\EventListener\\ResponseListener::filter(ResponseEvent | continued\n\$event): void",
        ),
    ];
    $domainListeners = [
        listener(
            'example.order.created',
            100,
            'Example\\EventListener\\OrderListener::notify',
            'Example\\EventListener\\OrderListener::notify(OrderCreatedEvent $event): void',
        ),
        listener(
            'example.order.created',
            0,
            'Example\\EventListener\\AuditListener::record',
            'Example\\EventListener\\AuditListener::record(OrderCreatedEvent $event): void',
        ),
    ];

    writeFixture('symfony-profiler-events-symfony-6.3.gz', 'e71e17', [
        'event_dispatcher' => [
            'called_listeners' => $mainListeners,
            'not_called_listeners' => [],
            'orphaned_events' => [],
        ],
        'domain_dispatcher' => [
            'called_listeners' => $domainListeners,
            'not_called_listeners' => [],
            'orphaned_events' => [],
        ],
    ]);
    writeFixture('symfony-profiler-events-symfony-6.2.gz', '1e9ac7', [
        'called_listeners' => array_slice($mainListeners, 0, 3),
        'not_called_listeners' => [],
        'orphaned_events' => [],
    ]);

    fwrite(STDOUT, 'Generated neutral Symfony events profiler fixtures with PHP ' . PHP_VERSION . PHP_EOL);
}
