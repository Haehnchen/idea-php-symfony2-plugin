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

    final class LoggerDataCollector
    {
        /** @param array<string, mixed> $data */
        public function __construct(protected Data $data)
        {
        }
    }
}

namespace {
    use Symfony\Component\HttpKernel\DataCollector\LoggerDataCollector;
    use Symfony\Component\VarDumper\Cloner\Data;

    /** @param list<array<string, mixed>> $logs */
    function addLog(
        array &$logs,
        int &$sequence,
        int $priority,
        string $priorityName,
        string $message,
        string $channel = 'app',
        ?bool $scream = null,
        int $errorCount = 1,
        ?string $type = null,
    ): void {
        ++$sequence;
        $log = [
            'timestamp' => 1_723_557_600 + $sequence,
            'timestamp_rfc3339' => sprintf('2026-08-16T12:00:%02d.000+00:00', $sequence),
            'priority' => $priority,
            'priorityName' => $priorityName,
            'channel' => $channel,
            'message' => $message,
            'context' => [
                'request_id' => sprintf('request-%02d', $sequence),
                'api_token' => 'fixture-secret-that-must-not-be-retained',
            ],
            'errorCount' => $errorCount,
        ];
        if ($scream !== null) {
            $log['scream'] = $scream;
        }
        if ($type !== null) {
            $log['type'] = $type;
        }
        $logs[] = $log;
    }

    // Synthetic messages intentionally exercise ordering, grouping, and per-level truncation.
    $logs = [];
    $sequence = 0;
    for ($index = 1; $index <= 30; ++$index) {
        addLog($logs, $sequence, 100, 'DEBUG', sprintf('Debug message %02d', $index));
    }
    addLog($logs, $sequence, 200, 'INFO', 'Application started');
    addLog($logs, $sequence, 200, 'INFO', 'Request matched', 'router');
    addLog($logs, $sequence, 250, 'NOTICE', 'Background refresh scheduled');
    for ($index = 1; $index <= 7; ++$index) {
        $message = 7 === $index ? "Warning message 07 | continued\nnext line" : sprintf('Warning message %02d', $index);
        addLog($logs, $sequence, 300, 'WARNING', $message);
    }
    addLog($logs, $sequence, 400, 'ERROR', 'First recoverable error');
    addLog($logs, $sequence, 400, 'ERROR', 'Latest recoverable error');
    addLog($logs, $sequence, 500, 'CRITICAL', 'Critical subsystem failure');
    addLog($logs, $sequence, 550, 'ALERT', 'Immediate operator action required');
    addLog($logs, $sequence, 600, 'EMERGENCY', 'System unavailable');
    addLog($logs, $sequence, 100, 'DEBUG', 'Deprecated feature used', 'deprecation', false);
    addLog($logs, $sequence, 100, 'DEBUG', 'Latest deprecated feature used', 'deprecation', null, 3, 'deprecation');
    addLog($logs, $sequence, 100, 'DEBUG', 'Suppressed fixture warning', 'php', true);

    $collector = new LoggerDataCollector(new Data([
        'error_count' => 5,
        'deprecation_count' => 4,
        'warning_count' => 7,
        'scream_count' => 1,
        'priorities' => [],
        'logs' => $logs,
        'compiler_logs' => [],
    ]));
    $profile = [
        'token' => '10ca11',
        'parent' => null,
        'children' => [],
        'data' => ['logger' => $collector],
        'ip' => '127.0.0.1',
        'method' => 'GET',
        'url' => 'http://example.test/logs',
        'time' => 1_723_557_600,
        'status_code' => 200,
    ];

    $serialized = serialize($profile);
    $roundTrip = unserialize($serialized, ['allowed_classes' => true]);
    if (!is_array($roundTrip) || !isset($roundTrip['data']['logger'])) {
        throw new RuntimeException('PHP rejected the generated profiler fixture');
    }

    $directory = __DIR__ . '/generated';
    if (!is_dir($directory) && !mkdir($directory, 0777, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create fixture directory');
    }

    $compressed = gzencode($serialized, 3);
    $target = $directory . '/symfony-profiler-logger.gz';
    if ($compressed === false || file_put_contents($target, $compressed) !== strlen($compressed)) {
        throw new RuntimeException('Unable to write profiler fixture');
    }

    fwrite(STDOUT, 'Generated neutral Symfony logger profiler fixture with PHP ' . PHP_VERSION . PHP_EOL);
}
