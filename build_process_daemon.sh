#!/bin/bash

# DataONE project builder utility.
# Handles building a project and projects its dependant on, for a 
# clean build-out.
#
# mvn clean, install is called in each project in PROJECTS array.
# Projects cleaned,installed in order of array, so order by dependency.
#
# jars in BUILDOUT_JARS are then copied to the BUILDOUT_DIR.
#
# Assumes buildout directory is rooted inside a fully checked out d1 src repository directory.
# svn checkout https://repository.dataone.org/software/cicore/trunk d1_src_trunk
#
# This script is specifically for d1_process_daemon, and so it assumes all paths relative to
# that directory
pwd
ROOT_PROJECTS=(d1_jibx_extensions d1_common_java d1_libclient_java d1_identity_manager)

NUM_ROOT_PROJECTS=${#ROOT_PROJECTS[@]}
#root of the trunk directory is two levels before d1_process_daemon
cd ../..
pwd
for ((i=0;i<$NUM_ROOT_PROJECTS;i++)); do
	PROJECT=${ROOT_PROJECTS[${i}]}
	echo ${PROJECT};
	cd ${PROJECT}
	mvn clean
	mvn -Dmaven.test.skip=true install
	cd ..
done

cd cn
pwd
CN_PROJECTS=(d1_cn_common d1_cn_noderegistry d1_log_aggregation d1_synchronization d1_replication d1_process_daemon)
NUM_CN_PROJECTS=${#CN_PROJECTS[@]}
#root of the trunk directory is two levels before d1_process_daemon
for ((i=0;i<$NUM_CN_PROJECTS;i++)); do
	PROJECT=${CN_PROJECTS[${i}]}
	echo ${PROJECT};
	cd ${PROJECT}
	mvn clean
	mvn -Dmaven.test.skip=true install
	cd ..
done

pwd
## copy the executable process daemon jar to buildout location

cp d1_process_daemon/d1_process_daemon.jar ../cn-buildout/dataone-cn-processdaemon/usr/share/dataone-cn-processdaemon



