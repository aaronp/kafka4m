https://deliciousbrains.com/ssl-certificate-authority-for-local-https-development/

and these guys did the same as me and created scripts:
https://github.com/kingkool68/generate-ssl-certs-for-local-development

https://gist.github.com/dobesv/13d4cb3cbd0fc4710fa55f89d1ef69be
# I like this:

[ -f $HOST_KEY ] || openssl genrsa -out $HOST_KEY 2048



use a command=line passphrase:
https://serverfault.com/questions/366372/is-it-possible-to-generate-rsa-key-without-pass-phrase