<?php

namespace Symfony\Component\Form
{
    final class FormEvents
    {
        /**
         *  - Add or remove form fields, before submitting the data to the form.
         * The event listener method receives a \Symfony\Component\Form\FormEvent instance.
         *
         * @Event
         */
        const PRE_SUBMIT = 'form.pre_bind';

        /**
         * @Event("\Foo\Bar")
         */
        const POST_SUBMIT = 'form.post_bind';
    }
}
