#!/bin/bash
cd .. || exit
java -cp bin NetworkManager new_group 3000 localhost 3000
cd scripts
echo "Press any key to continue . . ."
read -r var
