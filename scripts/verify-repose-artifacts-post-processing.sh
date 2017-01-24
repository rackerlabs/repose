#!/bin/bash
START=$(date +"%s")
echo -en "Starting at: $(date)\n"
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
VERIFY_DIR=${SCRIPT_DIR}/repose
#USER=user
#IP=192.168.1.100
#HOME=/home/user
USER=root
IP=50.56.173.100
HOME=/root

# Retreive the data that will be post processed.
scp -r ${USER}@${IP}:${HOME}/workspace/repose ${SCRIPT_DIR}
scp    ${USER}@${IP}:${HOME}/verify.txt       ${VERIFY_DIR}

# The assumption going into this was the following sequence was used:
#  - Pull the source from GitHub
#  - Compile the source into by byte-code (.java/.scala/.groovy/.jaxb => .class)
#  - Execute unit tests
#  - Package the byte-code into archives (.class => .jar)
#  - Package multiple archives along with configuration files into Enterprise/Web/Fat/Shaded archives (.jar => .ear/.war/.jar)
#  - Execute functional tests
#  - Package Enterprise/Web/Fat/Shaded archives into installation packages (.ear/.war/.jar => .deb/.rpm)
#
# This turned out to not be true and caused many itereations on the verify-repose-artifact(s) scripts.

# Post process the output of the verify-repose-artifact(s) scripts (e.g. ${VERIFY_DIR}/verify.txt) to identify all of the
# versions that were successful, those that were probably failed builds/deploys, and those that need further processing.
# - Grep pulls out the lines we care about.
# - Sed removes some unwanted text that was not separated by new lines.
# - Sed IF a line ends with elipses (e.g. ...)
#   AND the next line contains "SUCCESS" or "FAILURE",
#   THEN flatten it up a line.
# - Sed IF a line starts with "Testing",
#   AND the next line contains "SUCCESS"
#   THEN flatten it up a line.
# - Grep removes a lot of noise from this context.
# - Sed adds some blank lines for readability.
# - Grep extracts the pertinent information.
# - Grep removes all of the test products that are not shipped.
cat ${VERIFY_DIR}/verify.txt | \
grep \
 -e '^Processing Version v' \
 -e '^Testing ' \
 -e '^   /root/workspace/repose/' \
 -e 'SUCCESS' \
 -e 'FAILURE' | \
\
sed 's/Re-Checking .* SUCCESS/SUCCESS/' | \
sed 's/Re-Checking .* FAILURE/FAILURE/' | \
\
sed '/\.jar \.\.\. *$/{$!{N;s/\.jar \.\.\. *\n *SUCCESS/\.jar \.\.\. SUCCESS/;ty;P;D;:y}}' | \
sed '/\.ear \.\.\. *$/{$!{N;s/\.ear \.\.\. *\n *SUCCESS/\.ear \.\.\. SUCCESS/;ty;P;D;:y}}' | \
sed '/\.war \.\.\. *$/{$!{N;s/\.war \.\.\. *\n *SUCCESS/\.war \.\.\. SUCCESS/;ty;P;D;:y}}' | \
sed '/\.jar \.\.\. *$/{$!{N;s/\.jar \.\.\. *\n *FAILURE/\.jar \.\.\. FAILURE/;ty;P;D;:y}}' | \
sed '/\.ear \.\.\. *$/{$!{N;s/\.ear \.\.\. *\n *FAILURE/\.ear \.\.\. FAILURE/;ty;P;D;:y}}' | \
sed '/\.war \.\.\. *$/{$!{N;s/\.war \.\.\. *\n *FAILURE/\.war \.\.\. FAILURE/;ty;P;D;:y}}' | \
\
sed '/^Testing .*\.jar$/{$!{N;s/\.jar\n   \/root\/workspace\/repose\/[0-9].*\/[a-Z0-9\-]*\/[a-Z0-9\-]*[0-9].*\.jar ... SUCCESS/\.jar \.\.\. SUCCESS/;ty;P;D;:y}}' | \
sed '/^Testing .*\.ear$/{$!{N;s/\.ear\n   \/root\/workspace\/repose\/[0-9].*\/[a-Z0-9\-]*\/[a-Z0-9\-]*[0-9].*\.ear ... SUCCESS/\.ear \.\.\. SUCCESS/;ty;P;D;:y}}' | \
sed '/^Testing .*\.war$/{$!{N;s/\.war\n   \/root\/workspace\/repose\/[0-9].*\/[a-Z0-9\-]*\/[a-Z0-9\-]*[0-9].*\.war ... SUCCESS/\.war \.\.\. SUCCESS/;ty;P;D;:y}}' | \
\
grep -v \
 -e '^Retrieving RPM ' \
 -e '^Retrieving DEB ' \
 -e '   [a-Z0-9\-]* ... SUCCESS' \
 -e '   content-identity-auth-[12].[01] ... SUCCESS' \
 -e '   /root/workspace/repose/[0-9].*/[a-Z0-9\-]*/[a-Z0-9\-]*[0-9].*\.[jew]ar ... SUCCESS'| \
\
sed 's/^Processing Version v/\nProcessing Version v/g' | \
\
grep \
 -e '^$' \
 -e '^Processing Version v' \
 -e '^Testing ' | \
\
grep -v \
 -e ' \.\.\. SUCCESS' \
 -e 'Testing datastore-replicated-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing http-delegation-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing experimental-filter-bundle-[0-9].*.ear   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing mocks-servlet-[0-9].*.war   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing test-service-mock-[0-9].*.war   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing repose-spock-tests-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing servlet-contract-filter-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing exception-filter-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing test-container-support-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing test-tomcat-container-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing tightly-coupled-filter-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing mocks-util-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing repose-documentation-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
 -e 'Testing test-glassfish-container-[0-9].*.jar   NO KNOWN GOOD TO COMPARE' \
\
> ${VERIFY_DIR}/verify_excerpt.txt

# These are the versions that were identified in the excerpt produced above as NOT being identical in the archived,
# binary, or decompiled forms. The final version in this test was the first one that was published with signatures.
VERSIONS=(
 3.0.5
 3.0.6
 3.1.0
 3.1.1
 4.1.1
 4.1.4
 4.1.5
 5.0.0
 5.0.1
 5.0.2
 5.0.4
 5.0.5
 5.0.9
 6.0.0
 6.0.1
 6.0.2
 6.1.0.3
 6.1.1.0
 6.1.1.1
 6.2.0.1
 6.2.0.2
 6.2.0.3
 6.2.1.0
 6.2.2.0
 7.0.0.0
 7.0.0.1
 7.0.1.0
 7.0.1.1
 7.1.0.2
)

# Clean up from a previous execution.
find  ${VERIFY_DIR}/ -name verify.txt -prune -o -name "*.txt" -exec rm {} \;

# Remove some of the noise.
for version in ${VERSIONS[*]} ; do
 grep -v \
  -e '^ *Re-Checking' \
  -e 'differ$' \
  ${VERIFY_DIR}/${version}/verify.txt > ${VERIFY_DIR}/${version}/verify_cleaned.txt
done

# Split the diffs into left and right for additional processing.
echo -en "\n\nSplit the diffs into left and right..."
for version in ${VERSIONS[*]} ; do
 echo -en " ."
 grep "^<" ${VERIFY_DIR}/${version}/verify_cleaned.txt | cut -c2- > ${VERIFY_DIR}/${version}/left.txt
 grep "^>" ${VERIFY_DIR}/${version}/verify_cleaned.txt | cut -c2- > ${VERIFY_DIR}/${version}/right.txt
done

# Confirms the differences in the XmlElementRefs are just the decompilers ordering.
echo -en "\n\nConfirm the XMLElementsRefs were (de)compiler ordering..."
for version in ${VERSIONS[*]} ; do
 echo -en "\n   Processing Version v${version}..."
 for side in left right ; do
  grep \
   -e '@XmlElementRefs({' \
   ${VERIFY_DIR}/${version}/${side}.txt | \
  sed -r 's/@XmlElementRefs\(\{@/@XmlElementRefs\(\n    \{\n        @/g' | \
  sed -r 's/\), @/\)\n        @/g' | \
  sed -r 's/\)\}\)/\)\n    \}\)/g' | \
  sort -u > ${VERIFY_DIR}/${version}/${side}_sortedXmlElementRefs.txt
 done
 diff ${VERIFY_DIR}/${version}/left_sortedXmlElementRefs.txt ${VERIFY_DIR}/${version}/right_sortedXmlElementRefs.txt
 if [ $? -eq 0 ] ; then
  echo -en " SUCCESS"
 fi
done

# Remove the XmlElementRefs for additional processing.
echo -en "\n\nRemove some already tested information..."
for version in ${VERSIONS[*]} ; do
 echo -en " ."
 grep -v -e '@XmlElementRefs({' ${VERIFY_DIR}/${version}/verify_cleaned.txt > ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs.txt
done

# Confirms the differences in the bindings are just the (de)compiler ordering.
echo -en "\n\nConfirm binding errors were (de)compiler ordering..."
for version in ${VERSIONS[*]} ; do
 echo -en "\n   Processing Version v${version}..."
 for side in left right ; do
  grep \
   -e ' *<schemaBindings map=".*">' \
   -e ' *<package name=".*"/>' \
   -e ' *</schemaBindings>' \
   -e ' *<bindings scd=".*">' \
   -e ' *<class ref=".*"/>' \
   -e ' *</bindings>' \
   -e ' *</bindings>' \
   ${VERIFY_DIR}/${version}/${side}.txt | \
  sort -u > ${VERIFY_DIR}/${version}/${side}_sortedBindings.txt
 done
 diff ${VERIFY_DIR}/${version}/left_sortedBindings.txt ${VERIFY_DIR}/${version}/right_sortedBindings.txt
 if [ $? -eq 0 ] ; then
  echo -en " SUCCESS"
 fi
done

# Remove some of the JAXB insanity for additional processing.
echo -en "\n\nRemove some JAXB stuff..."
for version in ${VERSIONS[*]} ; do
 echo -en " ."
 grep -v \
 -e ' *Generated on:' \
 -e ' *<schemaBindings map=".*">' \
 -e ' *<package name=".*"/>' \
 -e ' *</schemaBindings>' \
 -e ' *<bindings scd=".*">' \
 -e ' *<class ref=".*"/>' \
 -e ' *</bindings>' \
 -e ' *</bindings>' \
 ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs.txt > ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB.txt
done

# Split the diffs again into left and right for additional processing.
echo -en "\n\nSplit the diffs again..."
for version in ${VERSIONS[*]} ; do
 echo -en " ."
 grep "^<" ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB.txt | sort -u | cut -c2- > ${VERIFY_DIR}/${version}/left_two.txt
 grep "^>" ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB.txt | sort -u | cut -c2- > ${VERIFY_DIR}/${version}/right_two.txt
 #diff ${VERIFY_DIR}/${version}/left_two.txt ${VERIFY_DIR}/${version}/right_two.txt
done

# Normalize a couple of variable names that seem to have (de)compiled slightly differently between the Nexus and RPM/DEB builds.
echo -en "\n\nNormalize a few variable names..."
for version in ${VERSIONS[*]} ; do
 echo -en "\n   Processing Version v${version}..."
 for side in left right ; do
  cat ${VERIFY_DIR}/${version}/${side}_two.txt | \
  sed 's/WebAppType/WebXyzType/g' | \
  sed 's/WebFragmentType/WebXyzType/g' | \
  sed 's/EjbRelationType/WebXyzType/g' | \
  sort -u \
  > ${VERIFY_DIR}/${version}/${side}_normalized.txt
 done
 diff ${VERIFY_DIR}/${version}/left_normalized.txt ${VERIFY_DIR}/${version}/right_normalized.txt
 if [ $? -eq 0 ] ; then
  echo -en " SUCCESS"
 fi
done

########################################################################################################################
# The end result is that the artifacts published to the Nexus repository for the                                       #
# following versions are the same as those publised to the RPM and DEB repositories.                                   #
#----------------------------------------------------------------------------------------------------------------------#
# - v3.0.5
# - v3.0.6
# - v3.1.0
# - v3.1.1
# - v4.1.1
# - v4.1.4
# - v4.1.5
# - v5.0.0
# - v5.0.1
# - v5.0.2
# - v5.0.4
# - v5.0.5
# - v5.0.9
# - v6.0.0
# - v6.0.1
# - v6.0.2
# - v6.1.0.3
# - v6.1.1.0
# - v6.1.1.1
# - v6.2.0.1
# - v6.2.0.2
# - v6.2.0.3
# - v6.2.1.0
# - v6.2.2.0
# - v7.0.0.0
# - v7.0.0.1
########################################################################################################################
# The artifacts published to the Nexus repository for the following versions are also the same, but did require a      #
# final human assessment of the output to confirm the initialization of the variables at first use vs. declaration.    #
#----------------------------------------------------------------------------------------------------------------------#
# - v7.0.1.0
# - v7.1.0.2
########################################################################################################################
# The final remaining artifact published to the Nexus repository does appear to have an issue, but it seems to be ours.#
# In addition to the human assessment of the output to confirm the initialization of the variables at first use vs.    #
# declaration, there is also a matter of missing comments in the XSD's. This by itself would not be cause for alarm,   #
# but it also appears the OpenStackServiceHeader enumeration is missing the CONTACT_ID which was introduced in this    #
# particular release. This is only seen in the RPM's and the DEB's are clean. This is not indicative of the release    #
# being maliciously manipulated, rather a failure of our deployment mechanism.                                         #
#----------------------------------------------------------------------------------------------------------------------#
# - v7.0.1.1
#----------------------------------------------------------------------------------------------------------------------#
#<    X_EXPIRATION("x-token-expires");
#---
#>    X_EXPIRATION("x-token-expires"),
#>    CONTACT_ID("X-CONTACT-ID");
########################################################################################################################

########################################################################################################################
# These additional removal and splitting steps are not necessary since sufficient paring down                          #
# of the data has already been accomplished and the status of the remaining builds determined.                         #
#----------------------------------------------------------------------------------------------------------------------#
## Remove the remainder of the JAXB insanity for additional processing.
#echo -en "\n\nRemove some more JAXB stuff..."
#for version in ${VERSIONS[*]} ; do
# echo -en " ."
# grep -v \
# -e '_WebAppType' \
# -e '_WebFragmentType' \
# -e '_EjbRelationType' \
# ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB.txt > ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB_two.txt
#done
#
## Split the diffs a third time into left and right for additional processing.
#echo -en "\n\nSplit the diffs a third time..."
#for version in ${VERSIONS[*]} ; do
# echo -en " ."
# grep "^<" ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB_two.txt | sort -u | cut -c2- > ${VERIFY_DIR}/${version}/left_too.txt
# grep "^>" ${VERIFY_DIR}/${version}/verify_NoXmlElementRefs_NoJAXB_two.txt | sort -u | cut -c2- > ${VERIFY_DIR}/${version}/right_too.txt
# #diff ${VERIFY_DIR}/${version}/left_too.txt ${VERIFY_DIR}/${version}/right_too.txt
#done
########################################################################################################################

unset VERSIONS

STOP=$(date +"%s") &&
DIFF=$(($STOP-$START)) &&
echo -en "\n\n" &&
echo -en "Total time: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n" &&
echo -en "Finished at: $(date)\n"
