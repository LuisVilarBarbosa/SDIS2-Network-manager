#!/bin/bash
cd .. || exit
java -classpath ".:sqlite-jdbc-3.16.1.jar" NetworkManager new_group 3000 localhost 3000
cd scripts
echo "Press any key to continue . . ."
read -r var
