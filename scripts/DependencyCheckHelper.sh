#!/bin/bash
#REPOSE_DIR=<REPOSE_DIR>/scripts
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
REPOSE_DIR=$SCRIPT_DIR/..
TARGET_DIR=$REPOSE_DIR/repose-aggregator/artifacts/build/dependencies

PROJECTS=(
    repose-aggregator:artifacts:cli-utils
    repose-aggregator:artifacts:experimental-filter-bundle
    repose-aggregator:artifacts:extensions-filter-bundle
    repose-aggregator:artifacts:filter-bundle
    repose-aggregator:artifacts:valve
)

cd $REPOSE_DIR
mkdir -p $TARGET_DIR

for project in ${PROJECTS[*]} ; do
    file=$(echo $project | sed s/\:/_/g)
    if [ ! -f "$TARGET_DIR/$file.txt" ]; then
        echo -e "\nBuilding the Dependency Tree for $project ..."
        gradle ${project}:dependencies > $TARGET_DIR/$file.txt
        sed -n '/default - Configuration for default artifacts./,$p' $TARGET_DIR/$file.txt | sed '/^$/q' | sed '/^$/d' > $TARGET_DIR/${file}2.txt
        mv $TARGET_DIR/${file}.txt $TARGET_DIR/${file}_ORIG.txt
        mv $TARGET_DIR/${file}2.txt $TARGET_DIR/$file.txt
        #cp -f $TARGET_DIR/${file}.txt $TARGET_DIR/${file}_EDIT.txt
        echo "Edit '$TARGET_DIR/$file.txt'"
        echo -e 'to flatten/remove all references to "project :repose-aggregator:".\n'
    else
        #cp -f $TARGET_DIR/${file}_EDIT.txt $TARGET_DIR/${file}.txt
        echo -e '\n================================================================================'
        echo "All of the direct dependencies for $project"
        echo 'that need the version numbers confirmed against what is documented:'
        echo '--------------------------------------------------------------------------------'
        egrep '^[+\]--- ' $TARGET_DIR/$file.txt | cut -d' ' -f2- | sed 's/ (\*)//g' | sort -u | sed 's/ -> //g' | sed 's/:/                         /g'
        echo -e '\n'
        #rm -f $TARGET_DIR/${file}.txt
    fi
done
