#!/bin/bash
cd .. || exit
java -cp "bin:sqlite-jdbc-3.16.1.jar" NetworkManager join_group 3002 localhost 3002 localhost 3000
cd scripts
echo "Press any key to continue . . ."
read -r var
