<?php

use Symfony\Component\Translation\MessageCatalogue;

$catalogue = new MessageCatalogue('de', array (
  'validators' => 
  array (
    'This value should be false.' => 'Dieser Wert sollte false sein.',
    'This value should be true.' => 'Dieser Wert sollte true sein.',
    'foo.escape' => 'A multiline translation ending with a ne\', wline.',
    'foo.bar' => 'A multiline translation ending with a newline.
',
    'foo.baz' => 'Dieser Wert sollte true sein.',
  ),
  'security' => 
  array (
    'An authentication exception occurred.' => 'Es ist ein Fehler bei der Authentifikation aufgetreten.',
    'Authentication credentials could not be found.' => 'Es konnten keine Zugangsdaten gefunden werden.',
  ),
  'pagination' => 
  array (
    'Next' => 'Vor',
    'Previous' => 'Zurück',
  ),
  'CraueFormFlowBundle' => 
  array (
    'button.next' => 'weiter',
  ),
  'FOSUserBundle' => 
  array (
    'registration.email.subject' => 'Willkommen %username%!',
    'registration.email.message' => 'Hallo %username%!

Besuche bitte folgende Seite, um Dein Benutzerkonto zu bestätigen: %confirmationUrl%

Mit besten Grüßen,
das Team.
',
    'resetting.password_already_requested' => 'Für diesen Benutzer wurde in den vergangenen 24 Stunden bereits ein neues Passwort beantragt.',
    'resetting.email.message' => 'Hallo %username%!

Besuche bitte folgende Seite, um Dein Passwort zurückzusetzen: %confirmationUrl%

Mit besten Grüßen,
das Team.
',
    'layout.logout' => 'Abmelden',
    'signup.headline_password' => 'Passwort',
  ),
  'welcome_login' =>
  array (
    'login.headline' => 'Anmeldung',
  ),
));

$catalogueEn = new MessageCatalogue('en', array (
  'validators' => 
  array (
    'This value should be false.' => 'This value should be false.',
    'This value should be true.' => 'This value should be true.',
    'fos_user.group.blank' => 'Please enter a name',
    'fos_user.group.short' => '[-Inf,Inf]The name is too short',
    'fos_user.group.long' => '[-Inf,Inf]The name is too long',
  ),
  'security' => 
  array (
    'An authentication exception occurred.' => 'An authentication exception occurred.',
    'Account is locked.' => 'Account is locked.',
  ),
  'pagination' => 
  array (
    'Next' => 'Next',
    'Previous' => 'Previous',
  ),
  'CraueFormFlowBundle' => 
  array (
    'button.next' => 'next',
    'button.finish' => 'finish',
    'button.back' => 'back',
    'button.reset' => 'start over',
  ),
  'FOSUserBundle' => 
  array (
    'Bad credentials' => 'Invalid username or password',
    'group.edit.submit' => 'Update group',
    'registration.email.message' => 'Hello %username%!

To finish activating your account - please visit %confirmationUrl%

Regards,
the Team.
',
    'resetting.password_already_requested' => 'The password for this user has already been requested within the last 24 hours.',
    'resetting.check_email' => 'An email has been sent to %email%. It contains a link you must click to reset your password.',
    'resetting.request.invalid_username' => 'The username or email address "%username%" does not exist.',
    'resetting.request.username' => 'Username or email address:',
    'resetting.request.submit' => 'Reset password',
    'resetting.reset.submit' => 'Change password',
    'resetting.flash.success' => 'The password has been reset successfully',
    'resetting.email.subject' => 'Reset Password',
    'resetting.email.message' => 'Hello %username%!

To reset your password - please visit %confirmationUrl%

Regards,
the Team.
',
    'layout.logout' => 'Logout',
  ),
));
$catalogue->addFallbackCatalogue($catalogueEn);


return $catalogue;
