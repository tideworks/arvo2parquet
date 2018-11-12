#!/bin/bash

if [[ "$OSTYPE" == 'msys' ]]; then
   sep=';'
else
   sep=':'
fi

dir=./target
cd $dir

export HOME=${PWD}
export HADOOP_HOME=${PWD}

CLASSPATH=$(find . -type f -name "*.jar")
CLASSPATH=$(echo ${CLASSPATH}|tr ' ' $sep)

RUN_CLASS=com.tideworks.data_load.DataLoad

java -ea -cp ".${sep}${CLASSPATH}" ${RUN_CLASS} $*
