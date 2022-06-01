<?php

use Symfony\Component\Translation\MessageCatalogue;

$catalogue = new MessageCatalogue('af', array (
  'validators' => 
  array (
    'This value should be false.' => 'Hierdie waarde moet vals wees.',
    'This value should be true.' => 'Hierdie waarde moet waar wees.',
  ),
  'security' =>
  array (
    'An authentication exception occurred.' => '\'n Verifikasie probleem het voorgekom.',
  ),
  'my_intl_icu_domain+intl-icu' =>
  array (
    'messages_intl_icu_key' => 'foobar intl-icu',
  ),
));

$catalogueEn = new MessageCatalogue('en', array (
  'validators' =>
  array (
    'This value should be false. (1)' => 'This value should be false.',
    'This value should be true. (1)' => 'This value should be true.',
  ),
));
$catalogue->addFallbackCatalogue($catalogueEn);

return $catalogue;
