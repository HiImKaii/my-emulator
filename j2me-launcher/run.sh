#!/bin/bash
# J2ME Launcher - Run Script

DIR="$(cd "$(dirname "$0")" && pwd)"
M2="$HOME/.m2/repository/org/openjfx"

# Build classpath with Linux JARs
CP="$DIR/target/j2me-launcher-1.0.0.jar"
CP="$CP:$M2/javafx-base/21.0.2/javafx-base-21.0.2-linux.jar"
CP="$CP:$M2/javafx-controls/21.0.2/javafx-controls-21.0.2-linux.jar"
CP="$CP:$M2/javafx-graphics/21.0.2/javafx-graphics-21.0.2-linux.jar"
CP="$CP:$M2/javafx-fxml/21.0.2/javafx-fxml-21.0.2-linux.jar"

# Run launcher
java --module-path "$CP" \
     --add-modules javafx.controls,javafx.graphics,javafx.fxml \
     -cp "$CP" \
     com.j2me.launcher.Main "$@"
