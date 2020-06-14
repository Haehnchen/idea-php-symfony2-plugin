<?php

namespace Symfony\Component\DependencyInjection\Loader\Configurator;

use Psr\Container\ContainerInterface;
use Symfony\Bundle\FrameworkBundle\CacheWarmer\TranslationsCacheWarmer;
use Symfony\Bundle\FrameworkBundle\Translation\Translator;
use Symfony\Component\Translation\Dumper\CsvFileDumper;
use Symfony\Component\Translation\Dumper\IcuResFileDumper;
use Symfony\Component\Translation\Dumper\IniFileDumper;
use Symfony\Component\Translation\Dumper\JsonFileDumper;
use Symfony\Component\Translation\Dumper\MoFileDumper;
use Symfony\Component\Translation\Dumper\PhpFileDumper;
use Symfony\Component\Translation\Dumper\PoFileDumper;
use Symfony\Component\Translation\Dumper\QtFileDumper;
use Symfony\Component\Translation\Dumper\XliffFileDumper;
use Symfony\Component\Translation\Dumper\YamlFileDumper;
use Symfony\Component\Translation\Extractor\ChainExtractor;
use Symfony\Component\Translation\Extractor\ExtractorInterface;
use Symfony\Component\Translation\Extractor\PhpExtractor;
use Symfony\Component\Translation\Formatter\MessageFormatter;
use Symfony\Component\Translation\Loader\CsvFileLoader;
use Symfony\Component\Translation\Loader\IcuDatFileLoader;
use Symfony\Component\Translation\Loader\IcuResFileLoader;
use Symfony\Component\Translation\Loader\IniFileLoader;
use Symfony\Component\Translation\Loader\JsonFileLoader;
use Symfony\Component\Translation\Loader\MoFileLoader;
use Symfony\Component\Translation\Loader\PhpFileLoader;
use Symfony\Component\Translation\Loader\PoFileLoader;
use Symfony\Component\Translation\Loader\QtFileLoader;
use Symfony\Component\Translation\Loader\XliffFileLoader;
use Symfony\Component\Translation\Loader\YamlFileLoader;
use Symfony\Component\Translation\LoggingTranslator;
use Symfony\Component\Translation\Reader\TranslationReader;
use Symfony\Component\Translation\Reader\TranslationReaderInterface;
use Symfony\Component\Translation\Writer\TranslationWriter;
use Symfony\Component\Translation\Writer\TranslationWriterInterface;
use Symfony\Contracts\Translation\TranslatorInterface;

return static function (ContainerConfigurator $container) {
    $container->services()
        ->set('translator.default', Translator::class)
        ->args([
            service_locator([]), // translation loaders locator
            service('translator.formatter'),
            param('kernel.default_locale'),
            abstract_arg('translation loaders ids'),
            [
                'cache_dir' => param('kernel.cache_dir').'/translations',
                'debug' => param('kernel.debug'),
            ],
            abstract_arg('enabled locales'),
        ])
        ->call('setConfigCacheFactory', [service('config_cache_factory')])
        ->tag('kernel.locale_aware')

        ->alias(TranslatorInterface::class, 'translator')

        ->set('translator.logging', LoggingTranslator::class)
        ->args([
            service('translator.logging.inner'),
            service('logger'),
        ])
        ->tag('monolog.logger', ['channel' => 'translation'])

        ->set('translation.warmer', TranslationsCacheWarmer::class)
        ->args([service(ContainerInterface::class)])
        ->tag('container.service_subscriber', ['id' => 'translator'])
        ->tag('kernel.cache_warmer')
    ;
};