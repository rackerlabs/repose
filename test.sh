#!/bin/bash

echo "This is the perf_test param: $perf_test"
echo "This is the extra_vars param: $extra_vars"
for i in $(seq 5); do echo "$i "; sleep 1; done

if [ "$(( RANDOM % 3 ))" -eq "0" ]; then
   echo "I feel like failing.";
   exit 1;
fi

echo "Success!"
