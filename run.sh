#!/bin/bash

if [[ "$OSTYPE" == 'msys' ]]; then
   sep=';'
else
   sep=':'
fi

export HOME=${PWD}
dir=./target
clspath=`find ${dir}/ -type f -name "*.jar"`
clspath=`echo ${clspath}|tr ' ' $sep`
cls=com/tideworks/data_load/DataLoad

java -ea -cp "${clspath}" ${cls} $*
