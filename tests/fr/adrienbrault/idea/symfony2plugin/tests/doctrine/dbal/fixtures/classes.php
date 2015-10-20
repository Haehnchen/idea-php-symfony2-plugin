<?php

namespace Doctrine\DBAL\Query
{
    interface QueryBuilder
    {
        public function update();
        public function insert();
        public function from();

        public function innerJoin();
        public function leftJoin();
        public function join();
        public function rightJoin();
    }
}