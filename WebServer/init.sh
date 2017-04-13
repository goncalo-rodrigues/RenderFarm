#!/bin/bash

ls_res=$(ls)
if [[ ${ls_res} != *"raytracer-master"* ]]; then
  #wget http://groups.ist.utl.pt/meic-cnv/project/raytracer-master.tgz
  #tar xvzf raytracer-master.tgz
  wget https://github.com/idris/raytracer/archive/master.zip
  mv master.zip raytracer-master.zip
  unzip raytracer-master.zip
  rm raytracer-master.zip
  cd raytracer-master
  for i in "*.txt"; do sed -i s/\\./,/g $i; done
  for i in "*.txt"; do sed -i s/,bmp/\\.bmp/g $i; done
  make
  cd ..
fi

cp *.java raytracer-master
cd raytracer-master
javac -cp ../raytracer-master/src *.java
echo
echo "Starting WebServer..."
java_bin=$(which java)
log=../server.log
sudo rm $log
sudo $java_bin -cp ~/CNV/raytracer-master/src:~/CNV/BIT:~/CNV/BIT/samples:. -XX:-UseSplitVerifier WebServer > >(tee -a $log) 2> >(tee -a $log >&2)
cd ..
