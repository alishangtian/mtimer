#!/bin/bash
if [ $# != 2 ] ; then
echo "USAGE: $0"
echo " e.g.: $0 {服务器ip} {集群名称}"
exit 1;
fi
git fetch --all && git reset --hard origin/master && chmod +x run.sh
mvn clean package -Dmaven.test.skip=true
pid=$(ps aux | grep mtimer-broker |grep -v grep| tr -s ' '| cut -d ' ' -f 2)
echo "pid is $pid shutdown now"
kill -9 $pid
nohup java -Dmtimer.broker.host=$1 -Dmtimer.broker.clusterName=$2 -Dmtimer.broker.startScanner=true -Xmx1024m -Xms512m -XX:+UseG1GC -verbose:gc -Xloggc:/usr/log/xtimer.gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintPromotionFailure -XX:+PrintGCApplicationStoppedTime -XX:+PrintHeapAtGC -jar mtimer-broker-0.0.1.release.jar >/dev/null 2>&1 &