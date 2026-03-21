#!/bin/bash
# Run FreeJ2ME with explicit classpath to fix classloader issues

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# Build classpath with FreeJ2ME classes
CP="build/freej2me.jar"
if [ -d "lib" ]; then
    for jar in lib/*.jar; do
        CP="$CP:$jar"
    done
fi

# Run with explicit classpath
java -cp "$CP" org.recompile.freej2me.FreeJ2ME "$@"
