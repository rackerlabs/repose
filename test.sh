#!/bin/bash

echo "This is the perf_test param: $perf_test"
echo "This is the extra_vars param: $extra_vars"
for i in $(seq 10); do echo "$i "; sleep 1; done
echo "Done"
