#!/bin/bash

TEMPFILE="/tmp/md5check"
OS=`uname`

usage(){
  echo "Usage $0 hostname bitcask.jar [bitcask directory]"
}

urlencode() {
  echo -n "${1}" | perl -pe 's/([^-_.~A-Za-z0-9])/sprintf("%%%02X", ord($1))/seg';
}

invokecli(){
  java -jar $JAR "${1}" | grep microseconds | cut -d, -f 5,6,8,9,10 |
  while IFS=, read -r -a line ; do
    BUCKET=${line[0]}
    KEY="${line[1]}"
    SIZE="${line[2]}"
    MD5="${line[3]}"
    btdata="${line[4]}"
    URI=buckets/"$(urlencode "$BUCKET")"/keys/"$(urlencode "$KEY")";
    read -r -a result <<< $(curl -X GET -o $TEMPFILE -w "%{http_code} %{size_download}" -s "http://$hostname:8098/$URI")
    RETCODE="${result[0]}"
    RETSIZE="${result[1]}"
    if [ "$RETCODE" == "404" ]
    then
      if [ "$SIZE" == "0" ]
      then
        echo "404, Tombstone Found - "$BUCKET $KEY
      else
        echo "404, BAD SIZE $SIZE/$RETSIZE  - "$BUCKET $KEY 
      fi
    else
      TEMPMD5=`eval "$CMD"`
      if [ "$MD5" != "$TEMPMD5" ]
      then
        echo 200 BAD MD5, $CMD \("$MD5" / $TEMPMD5\) \(  $SIZE / $RETSIZE \) ",$btdata,"
      #else
      #  echo OK
      fi
    fi
  done
}

if [ $# -lt 2 ]; then
  usage
  exit 1;
fi

hostname="$1"
`host $hostname >/dev/null`
if [ $? -ne 0 ]; then
  echo Host $hostname not found
  exit 2;
fi

JAR="$2"
if [ ! -f "$JAR" ]; then 
  echo "Argument 2 \"$JAR\" is not a regular file"
  usage
  exit 1;
fi

if [ `uname` == "Darwin" ]; then
  CMD="md5 $TEMPFILE | tr -s ' ' | cut -d ' ' -f 4"
else
  CMD="md5sum $TEMPFILE | tr -s ' ' | cut -d ' ' -f 1"
fi

if [ "$3" == "" ]; then
  for i in $( find . -name \*bitcask.data ); do
    echo "Processing $i"
    invokecli "$i";
  done
elif [ -d "$3" ]; then 
  for i in $( find "$3" -name \*bitcask.data ); do
    echo "Processing $i"
    invokecli "$i";
  done
elif [ -f "$3" ]; then
  invokecli "$3";
fi
