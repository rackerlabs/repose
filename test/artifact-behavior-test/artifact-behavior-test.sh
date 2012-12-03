#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RESPONSE_HEADERS=$DIR/responseHeaders
RESPONSE_BODY=$DIR/responseBody
REPOSE_PORT=8888

if [ -z "$1" ]; then
    CONFIG_DIR=/etc/repose
else
    CONFIG_DIR=$1
fi

CONTAINER_CFG="$CONFIG_DIR/container.cfg.xml"


BASE_DIR=$( cat $CONTAINER_CFG | sed -n "s_[ \t]*<\(.*\)deployment-directory.*>\(.*\)</\1deployment-directory>_\2_p" )
ART_DIR=$( cat $CONTAINER_CFG | sed -n "s_[ \t]*<\(.*\)artifact-directory.*>\(.*\)</\1artifact-directory>_\2_p" )

echo "start..."

if [ ! -d "$BASE_DIR" ]; then
    echo $BASE_DIR is not a valid directory.  Check your container.cfg.xml
    exit 3;
fi

echo Deployment directory: $BASE_DIR
echo Artifact directory: $ART_DIR


FILTER_A_BUNDLE="`pwd`/../../project-set/external/testing/dummy-filters/dummy-filter-a/dummy-filter-bundle-a/target/"
FILTER_B_BUNDLE="`pwd`/../../project-set/external/testing/dummy-filters/dummy-filter-b/dummy-filter-bundle-b/target/"
DROP_CONFIGS="`pwd`/system-models/"

echo "Filter Bundle A: $FILTER_A_BUNDLE"
echo "Filter Bundle B: $FILTER_B_BUNDLE"

sendRequest()
{
    RESPONSE=`curl -s -D $RESPONSE_HEADERS  -w "%{http_code}" localhost:8888/service -H "x-trace-request:true" -o $RESPONSE_BODY`

    echo "Response Code: $RESPONSE"
    if [ ! $RESPONSE -eq 200 ]
    then
        echo "ERROR"
    #    cat $RESPONSE_BODY
#    else
#        echo "PASS"
    fi

    if [ -f $RESPONSE_BODY ]
    then
        rm $RESPONSE_BODY
    fi
}

checkFilters()
{
    NUMFILTERS=`awk '/X\-.+Time:/{n++}; END {print n}' responseHeaders`
    NUMFILTERS=$(expr $NUMFILTERS - 1)
    echo "Number of filters: $NUMFILTERS"
    if [ $NUMFILTERS != "0" ] && [ $1 == "Empty" ]
    then
        echo "ERROR"
    #    cat $DIR/responseHeaders
  #  else
  #      echo "PASS"
    fi

    if [ -f $DIR/responseHeaders ]
    then
        rm $DIR/responseHeaders
    fi

}



echo "1) Start repose with no ear artifacts at all.  Pass through system model. List available filters (see XXXX), confirm that list is empty."
### This test assumes that Repose is currently running with a blank filter list and no ears in the artifact directory

sendRequest
checkFilters "Empty"

echo "2) Introduce an ear file with filter A. The filter simply response with "A" when someone issues a GET on anything."

cp $FILTER_A_BUNDLE/*.ear $ART_DIR/filter-a.ear
sleep 20
sendRequest
checkFilters "Empty"

echo "#3) Modify system model to include filter A.  Assert that list of filters contains Version A."

cp $DROP_CONFIGS/system-model-pass-a.xml /etc/repose/system-model.cfg.xml
sleep 20
sendRequest
checkFilters "NotEmpty"

echo "4) Introduce an ear file with filter B. This ear file replaces the previous ear.  This filter simply response with "B" Assert that list of filters contains B."
rm $ART_DIR/filter-a.ear
cp $FILTER_B_BUNDLE/*.ear $ART_DIR/filter-b.ear
sleep 40
sendRequest

echo "#5) Remove filter from system model."
cp $DROP_CONFIGS/system-model-pass-thru.xml /etc/repose/system-model.cfg.xml
sleep 20
sendRequest

echo "#6) Remove filter ear file."
rm $ART_DIR/filter-b.ear
sleep 20
sendRequest



#rm $ART_DIR/*.ear
cp $DROP_CONFIGS/system-model-pass-thru.xml /etc/repose/system-model.cfg.xml
