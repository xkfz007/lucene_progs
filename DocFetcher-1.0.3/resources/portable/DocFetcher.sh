#!/bin/sh

script=$(readlink -f "$0")
scriptdir=`dirname "$script"`
cd "$scriptdir"

chmod a+x ./docfetcher-daemon-linux

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

for FILE in `ls ./lib/linux/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

CLASSPATH=${CLASSPATH}:./lang

export LD_LIBRARY_PATH="./lib"
export GDK_NATIVE_WINDOWS=1

java -Xmx256m -cp ".:${CLASSPATH}" -Djava.library.path="lib" net.sourceforge.docfetcher.DocFetcher "$@"
