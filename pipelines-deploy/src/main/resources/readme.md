Run with:

```bash
docker run -it \
 --rm \
 --name pipelines \
 -p 80:80 \
 -v `pwd`/target/certificates:/app/certs \
 pipelines:latest \
 pipelines.tls.certificate=/app/certs/cert.p12 \
 pipelines.tls.password=password \
 pipelines.tls.seed=meh
```

or 

```bash
docker run -it \
 --rm \
 --name pipelines \
 -p 80:80 \
 -v `pwd`/target/certificates:/app/certs \
 pipelines:latest 
```
