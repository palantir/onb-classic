#!/bin/bash

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Change to the script directory
cd "$SCRIPT_DIR"

if hash java 2>/dev/null; then
    echo "ONB-Classic requires sudo rights to bind to low-level ports (67, 69, 4011)."
    echo "You will be prompted for your password."
    echo ""
    sudo java -jar "onb-classic-all.jar" $@
else
    if [ ! -d "jre" ]; then
        echo 'Appears there is no java, please download java first.'
    fi
fi

echo ""
echo "Press Enter to close this window..."
read
