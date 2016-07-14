# bitcask-cli
Cli tool to dump bitcask content, now able to search for specific keys in vhosts

# Usage
```$ bash search.sh  ```

the bash script search.sh execute the jar for bitcasks files under /data/riak/bitcasks

```$ java -jar bitcask-x-x-x.jar -k keys -l info <vhost>```

* -k is a \t separated file with bucket \t key to search
* -l info set the logger level in java format

# Build
Build executable jar under target/ ```mvn package```  
You need to have jbert (https://github.com/bejelith/jbert) as dependency to build this project

# Bitcask design
http://basho.com/wp-content/uploads/2015/05/bitcask-intro.pdf
