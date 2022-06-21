#!/bin/bash

mkdir -p /kb-importer/data
cd /kb-importer/data
if [ -f /kb-importer/kb-importer.jar ]; then
  mv /kb-importer/kb-importer.jar /kb-importer/kaybee /kb-importer/data
fi

#substitute env variables used by kaybee in kaybeeconf.yaml
sed "s|KB_IMPORTER_STATEMENTS_REPO|$KB_IMPORTER_STATEMENTS_REPO|g" ../conf/kaybeeconf.yaml.sample > ../conf/kaybeeconf.yaml
sed -i "s|KB_IMPORTER_STATEMENTS_BRANCH|$KB_IMPORTER_STATEMENTS_BRANCH|g" ../conf/kaybeeconf.yaml

echo "Statements repo: " $KB_IMPORTER_STATEMENTS_REPO

echo "Statements branch: " $KB_IMPORTER_STATEMENTS_BRANCH
echo "Statements folder: " $KB_IMPORTER_STATEMENTS_FOLDER
echo "Skip clones: " $KB_IMPORTER_SKIP_CLONE

#Adding certs
certs=`ls /kb-importer/certs | grep -v readme.txt`
for cert in $certs; do
   keytool -import -alias $cert -storepass changeit -keystore /usr/lib/jvm/java-1.8-openjdk/jre/lib/security/cacerts -file /kb-importer/certs/$cert -noprompt
done

(java -Dvulas.shared.backend.header.X-Vulas-Client-Token=$BACKEND_BUGS_TOKEN \
            -Dvulas.shared.cia.serviceUrl=$CIA_SERVICE_URL \
            -Dvulas.shared.backend.serviceUrl=$BACKEND_SERVICE_URL \
            -jar /kb-importer/data/kb-importer.jar | tee analyzer_logs.txt ) &

#Wait for kb-importer and backend to start
sleep 40

curl localhost:8080/start?skipClone=$KB_IMPORTER_SKIP_CLONE -X POST
