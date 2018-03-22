<?php

namespace Symfony\Component\Security\Core\Authorization\Voter
{
    interface VoterInterface
    {
        public function vote(TokenInterface $token, $subject, array $attributes);
    }
    abstract class Voter implements VoterInterface
    {
        abstract protected function voteOnAttribute($attribute, $subject, TokenInterface $token);
        abstract protected function supports($attribute, $subject);
    }
}

namespace
{
    use Symfony\Component\Security\Core\Authorization\Voter\Voter;

    class MyVoter extends Voter
    {
        const FOOBAR = 'FOOBAR';
        const FOOBAR_1 = 'FOOBAR_1';
        const FOOBAR_2 = 'FOOBAR_2';
        const FOOBAR_3 = 'FOOBAR_3';
        const FOOBAR_4 = 'FOOBAR_4';
        const FOOBAR_5 = 'FOOBAR_5';

        const FOOBAR_ARRAY_1 = 'FOOBAR_ARRAY_1';
        const FOOBAR_ARRAY_2 = 'FOOBAR_ARRAY_2';
        const FOOBAR_ARRAY_3 = 'FOOBAR_ARRAY_3';

        const FOOBAR_CASE_1 = 'FOOBAR_CASE_1';
        const FOOBAR_CASE_2 = 'FOOBAR_CASE_2';

        const FOOBAR_IF_1 = 'FOOBAR_IF_1';
        const FOOBAR_IF_2 = 'FOOBAR_IF_2';
        const FOOBAR_IF_3 = 'FOOBAR_IF_3';
        const FOOBAR_IF_4 = 'FOOBAR_IF_4';

        const FOOBAR_ATTRIBUTES_IN_CONST = [
            'FOOBAR_ATTRIBUTES_IN_CONST_1',
        ];

        private $foo = [
            'FOOBAR_ATTRIBUTES_IN_PROPERTY_1',
        ];

        /**
         * {@inheritdoc}
         */
        protected function supports($attribute, $subject)
        {
            return $subject instanceof \DateTime && in_array($attribute, [self::FOOBAR_ARRAY_3, 'FOOBAR_ARRAY_4']);
        }

        /**
         * {@inheritdoc}
         */
        protected function voteOnAttribute($attribute, $subject, TokenInterface $token)
        {
            in_array($attribute, [self::FOOBAR_ARRAY_1, self::FOOBAR_ARRAY_2]);
            in_array($attribute, self::FOOBAR_ATTRIBUTES_IN_CONST);
            in_array($attribute, $this->foo);

            switch ($attribute) {
                case self::FOOBAR_CASE_1:
                    break;
                case self::FOOBAR_CASE_2:
                    break;
            }

            if($attribute === self::FOOBAR_IF_1) { }
            if(self::FOOBAR_IF_2 === $attribute) { }

            if($attribute == self::FOOBAR_IF_3) { }
            if(self::FOOBAR_IF_4 == $attribute) { }
        }
    }

    use Symfony\Component\Security\Core\Authorization\Voter\VoterInterface;

    class MyVoterEach implements VoterInterface
    {
        const FOOBAR_EACH_1 = 'FOOBAR_EACH_1';
        const FOOBAR_ATTRIBUTES_IN_ARRAY = 'FOOBAR_ATTRIBUTES_IN_ARRAY';

        public function vote(TokenInterface $token, $subject, array $attributes)
        {
            in_array(self::FOOBAR_ATTRIBUTES_IN_ARRAY, $attributes, true);

            foreach ($attributes as $attribute) {
                if (in_array($attribute, [self::FOOBAR_EACH_1], true)) {}
            }
        }
    }
}