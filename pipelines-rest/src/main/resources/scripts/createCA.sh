#!/usr/bin/env bash


trap "cleanupCA" EXIT

# some lovely shared variables
export CA_DIR=${CA_DIR:-target/ca}
mkdir -p ${CA_DIR}

export CA_DOMAIN=${CA_DOMAIN:-`hostname`}

export CA_PWFILE=${CA_PWFILE:-"$CA_DIR/capass.txt"}
export CA_DEFAULT_PWD=${CA_DEFAULT_PWD:-"password"}
export CA_PRIVATE_KEY_FILE=${CA_PRIVATE_KEY_FILE:-"$CA_DIR/secret.key"}
export CA_PUBLIC_KEY_FILE=${CA_PUBLIC_KEY_FILE:-"$CA_DIR/secret.pub"}
export CA_DETAILS_FILE=${CA_DETAILS_FILE:-$CA_DIR/ca-options.conf}
export CA_FILE=${CA_FILE:-"$CA_DIR/${CA_DOMAIN}-ca.crt"}

# these are used to create the default 'CA_DETAILS_FILE' if it's not specified
export CA_DETAILS_C=${CA_DETAILS_C:-GB}
export CA_DETAILS_ST=${CA_DETAILS_ST:-London}
export CA_DETAILS_L=${CA_DETAILS_L:-London}
export CA_DETAILS_O=${CA_DETAILS_O:-End Point}
export CA_DETAILS_OU=${CA_DETAILS_OU:-Testing Domain}
export CA_DETAILS_emailAddress=${CA_DETAILS_emailAddress:-your-administrative-address@your-awesome-existing-domain.com}


CA_CREATED_PW_FILE=false
INFO=">>> "
# ensure there is a $CA_PWFILE
ensureCAPassword () {
	CA_CREATED_PW_FILE=false
	if [[ ! -f ${CA_PWFILE}  ]]; then
	  CA_CREATED_PW_FILE=true
	  echo "$CA_PWFILE doesn't exist, creating default password..."
	  echo ${CA_DEFAULT_PWD} > ${CA_PWFILE}
	else
	  echo "Using pw file $CA_PWFILE"
	fi
}

# remove the password if it was created
cleanupCA () {
	if [[ ${CA_CREATED_PW_FILE}="true" ]];then
	  echo "removing $CA_PWFILE"
	  rm ${CA_PWFILE}
	else
		echo done
	fi
}


# create private/public keys
ensureCAKeys () {

	echo "+ + + + + + + + + + + + + + + Ensuring CA key pair + + + + + + + + + + + + + + + "

    if [[ ! -f ${CA_PRIVATE_KEY_FILE} ]];then
		echo "$INFO Private key '$CA_PRIVATE_KEY_FILE' doesn't exist, creating"

    	ensureCAPassword
		openssl genrsa -aes128 -passout file:${CA_PWFILE}  -out ${CA_PRIVATE_KEY_FILE} 3072
	else
        echo "$INFO Private key '$CA_PRIVATE_KEY_FILE' already exists"
	fi

    if [[ ! -f ${CA_PUBLIC_KEY_FILE} ]];then
		echo "$INFO public key '$CA_PUBLIC_KEY_FILE' doesn't exist, creating"

    	ensureCAPassword
		openssl rsa -in ${CA_PRIVATE_KEY_FILE} -passin file:${CA_PWFILE} -pubout -out ${CA_PUBLIC_KEY_FILE}
	else
		echo "$INFO public key $CA_PUBLIC_KEY_FILE already exists"
	fi
}

ensureCASubject () {
    if [[ ! -f ${CA_DETAILS_FILE} ]];then
		echo "$INFO CA details CA_DETAILS_FILE '$CA_DETAILS_FILE' doesn't exist, creating"

    		cat > ${CA_DETAILS_FILE} <<-EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[ dn ]
C=$CA_DETAILS_C
ST=$CA_DETAILS_ST
L=$CA_DETAILS_L
O=$CA_DETAILS_O
OU=$CA_DETAILS_OU
emailAddress=$CA_DETAILS_emailAddress
CN = $CA_DOMAIN

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = ${CA_DOMAIN}
DNS.2 = www.${CA_DOMAIN}
DNS.3 = localhost
EOF
	else
        echo "$INFO CA_DETAILS_FILE ${CA_DETAILS_FILE} already exists"
	fi
}

# ensures the CA_FILE exists or creates one if necessary
ensureCA () {

	echo "+ + + + + + + + + + + + + + + Ensuring root CA file $CA_FILE + + + + + + + + + + + + + + + "
	if [[ ! -f ${CA_FILE} ]];then
		echo "$INFO Certificate Authority file '$CA_FILE' doesn't exist, creating with subject $SUBJECT"

	    ensureCAKeys
	    ensureCAPassword
	    ensureCASubject

        #https://deliciousbrains.com/ssl-certificate-authority-for-local-https-development/
        #https://www.endpoint.com/blog/2014/10/30/openssl-csr-with-alternative-names-one
	    openssl req -x509 -new -nodes -key ${CA_PRIVATE_KEY_FILE} -passin file:$CA_PWFILE -sha256 -days 1825 -out ${CA_FILE} -config <( cat $CA_DETAILS_FILE )
	else
		echo "$INFO CA_FILE '${CA_FILE}'' exists, skipping"
	fi
}



