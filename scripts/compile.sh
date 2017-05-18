#!/bin/bash
cd .. || exit
mkdir -p bin
echo "Compiling..."
javac -d bin NetworkManager.java
cd scripts
echo "Press any key to continue . . ."
read -r var
