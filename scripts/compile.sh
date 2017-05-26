#!/bin/bash
cd .. || exit
mkdir -p bin
echo "Compiling..."
javac -classpath ".:sqlite-jdbc-3.16.1.jar" -d bin NetworkManager.java
cd scripts
echo "Press any key to continue . . ."
read -r var
