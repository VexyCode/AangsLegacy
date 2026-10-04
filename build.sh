#!/usr/bin/env bash

set -e

SERVER="/home/Vexor/.cache/JetBrains/IntelliJIdea2026.2/com.nickawrist.MinecraftDevServer/al_test"
PLUGIN="$SERVER/plugins/aangs-legacy-beta.jar"

echo "==> Building Aang's Legacy..."
mvn clean package

echo "==> Installing plugin..."
JAR=$(find target -maxdepth 1 -type f -name 'aangs-legacy-*-beta.*.jar' | head -n 1)

if [[ -z "$JAR" ]]; then
    echo "ERROR: Could not find the built plugin JAR."
    exit 1
fi

cp "$JAR" "$PLUGIN"

echo "==> Installed:"
echo "    $JAR"
echo "    -> $PLUGIN"

echo
printf "Delete ALL test worlds and regenerate them? [y/N] "
read -r ANSWER

if [[ "$ANSWER" =~ ^[Yy]$ ]]; then
    echo "==> Removing test worlds..."
    rm -rf \
        "$SERVER/world" \
        "$SERVER/world_nether" \
        "$SERVER/world_the_end"

    echo "==> All worlds will be regenerated on next server start."
else
    echo "==> Keeping existing worlds."
fi

echo
echo "==> Done."