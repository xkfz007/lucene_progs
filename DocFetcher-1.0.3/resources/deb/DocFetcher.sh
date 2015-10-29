#!/bin/sh

cd /usr/share/docfetcher

DDFILE=docfetcher-daemon-linux.desktop
if [ ! -f ~/.config/autostart/$DDFILE ]; then
	cp ./$DDFILE ~/.config/autostart
fi

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

CLASSPATH=${CLASSPATH}:./lang

export LD_LIBRARY_PATH="./lib"
export GDK_NATIVE_WINDOWS=1

java -Xmx256m -cp ".:${CLASSPATH}" -Djava.library.path="lib" net.sourceforge.docfetcher.DocFetcher "$@"
