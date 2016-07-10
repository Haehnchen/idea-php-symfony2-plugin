<?php

namespace Doctrine\DBAL\Query
{
    interface QueryBuilder
    {
        public function update();
        public function insert();
        public function from();
        public function delete();

        public function innerJoin();
        public function leftJoin();
        public function join();
        public function rightJoin();
    }
}

namespace Doctrine\DBAL
{
    interface Connection
    {
        public function insert();
        public function update();
    }
}