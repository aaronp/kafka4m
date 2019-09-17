Building
======
This project is built using sbt:

```$xslt
sbt -mem 4096 clean test docker
```

This will create a docker image which can launch the Kafka4mApp.

Publishing is currently managed locally:

e.g.

```
$ docker login
<credentials>
$ docker tag abcdefg123456789 porpoiseltd/kafka4m:dev0.0.0
$ docker push porpoiseltd/kafka4m:dev0.0.0
```


## Releasing

To release a new version:

```$xslt
sbt release
```

