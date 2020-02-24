
choco install openjdk11 -params 'installdir=c:\\jdk' -y
export JAVA_HOME=${JAVA_HOME:-/c/jdk}
export PATH=${JAVA_HOME}/bin:${PATH}