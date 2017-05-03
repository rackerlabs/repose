#!/bin/bash

echo "This is the perf_test param: $perf_test"
echo "This is the extra_vars param: $extra_vars"

if [ "$(( BUILD_NUMBER % 2 ))" -eq "0" ]; then
   echo "I feel like failing.";
   exit 1;
fi

echo "Success!"
