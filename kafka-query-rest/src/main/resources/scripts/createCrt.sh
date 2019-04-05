#!/usr/bin/env bash

source ./createCA.sh


trap "cleanCrt" EXIT

# some lovely shared variables
export CRT_DIR=${CRT_DIR:-target/crt}
mkdir -p ${CRT_DIR}


export CRT_PWFILE=${PWFILE:-"$CRT_DIR/crtpass.txt"}
export CRT_DEFAULT_PWD=${CRT_DEFAULT_PWD:-"password"}
export CRT_NAME=${CRT_NAME:-`hostname`}
export DNS_NAME=${DNS_NAME:-"${CRT_NAME}"}
export CRT_KEY_FILE=${CRT_KEY_FILE:-"$CRT_DIR/$CRT_NAME.pem"}
export CRT_CSR_FILE=${CRT_CSR_FILE:-"$CRT_DIR/$CRT_NAME.csr"}
export CRT_CERT_FILE=${CRT_CERT_FILE:-"$CRT_DIR/$CRT_NAME.crt"}
export CRT_DETAILS_FILE=${CRT_DETAILS_FILE:-"$CRT_DIR/${CRT_NAME}-options.conf"}


# stuff for converting our CRT_CERT_FILE from .crt into a .jks file
export CRT_CERT_FILE_JKS=${CRT_CERT_FILE_JKS:-"$CRT_DIR/$CRT_NAME.jks"}
export CRT_CERT_FILE_JKS_ALIAS=${CRT_CERT_FILE_JKS_ALIAS:-$CRT_NAME}


# stuff for converting our CRT_CERT_FILE from .crt into a .p12 file
export CRT_CERT_FILE_P12=${CRT_CERT_FILE_P12:-"$CRT_DIR/$CRT_NAME.p12"}


#
# CRT Details
#
export CRT_CSR_DETAILS_FILE=${CRT_CSR_DETAILS_FILE:-"$CRT_DIR/${CRT_NAME}-csr.conf"}
# these are used to create the default 'CRT_CSR_DETAILS_FILE' if it's not specified
export CRT_DETAILS_C=${CRT_DETAILS_C:-GB}
export CRT_DETAILS_ST=${CRT_DETAILS_ST:-London}
export CRT_DETAILS_L=${CRT_DETAILS_L:-London}
export CRT_DETAILS_O=${CRT_DETAILS_O:-End Point}
export CRT_DETAILS_OU=${CRT_DETAILS_OU:-Testing Domain}
export CRT_DETAILS_emailAddress=${CRT_DETAILS_emailAddress:-your-administrative-address@your-awesome-existing-domain.com}

export CRT_JKS_PW=${CRT_JKS_PW:-changeThisPassword}

CRT_CREATED_PW_FILE=false
INFO=">>> "
# ensure ther is a $PWFILE
cleanCrt () {
	if [[ $CRT_CREATED_PW_FILE = "true" ]];then
	  echo "removing $CRT_PWFILE"
	  rm $CRT_PWFILE
	else
	  echo "no need to cleanup CRT "
	fi
}

ensureCRTPassword () {
	CRT_CREATED_PW_FILE=false
	if [[ ! -f ${CRT_PWFILE} ]]; then
	  CRT_CREATED_PW_FILE=true
	  echo "$INFO CRT_PWFILE $CRT_PWFILE doesn't exist, creating default password..."
	  echo "${CRT_DEFAULT_PWD}" > ${CRT_PWFILE}
	else
	  echo "$INFO Using pw file $CRT_PWFILE"
	fi
}

ensureCrtKey () {

	if [[ ! -f ${CRT_KEY_FILE} ]]; then
  		echo "$INFO creating CRT_KEY_FILE $CRT_KEY_FILE"
  		ensureCRTPassword
  		openssl genrsa -passout file:$CRT_PWFILE  -out ${CRT_KEY_FILE} 2048	
  	else
  		echo "$INFO CRT_KEY_FILE $CRT_KEY_FILE exists, skipping"
  	fi
}


ensureCRTSubject () {
    if [[ ! -f ${CRT_DETAILS_FILE} ]];then
		echo "$INFO CRT details '$CRT_DETAILS_FILE' doesn't exist, creating"

    		cat > ${CRT_DETAILS_FILE} <<-EOF
[req]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[ dn ]
C=$CRT_DETAILS_C
ST=$CRT_DETAILS_ST
L=$CRT_DETAILS_L
O=$CRT_DETAILS_O
OU=$CRT_DETAILS_OU
emailAddress=$CRT_DETAILS_emailAddress
CN = ${CRT_NAME}

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = ${DNS_NAME}
DNS.2 = localhost
EOF
	else
        echo "$INFO CRT_DETAILS_FILE ${CRT_DETAILS_FILE} already exists"
	fi
}

# Creates a new CSR file using the 'CRT Subject' provided by ensureCRTSubject and 
# encrypted using our CRT_KEY_FILE file
ensureCrtCR () {
	echo "+ + + + + + + + + + + + + + + Ensuring Cert CRT file $CRT_CSR_FILE + + + + + + + + + + + + + + + "
	if [[ ! -f ${CRT_CSR_FILE} ]]; then
  		echo "$INFO creating CRT_CSR_FILE $CRT_CSR_FILE"
  		ensureCrtKey
  		ensureCRTSubject
  		openssl req -new -key ${CRT_KEY_FILE} -out ${CRT_CSR_FILE} -config <( cat $CRT_DETAILS_FILE )
  	else
  		echo "$INFO CRT_CSR_FILE $CRT_CSR_FILE exists, skipping"
  	fi
}

# Once we have our cert signing request, we can sign it with our CA based on the 
ensureCrtCSRConfFile () {

	echo "+ + + + + + + + + + + + + + + Ensuring Cert CSR config file $CRT_CSR_DETAILS_FILE + + + + + + + + + + + + + + + "
	if [[ ! -f ${CRT_CSR_DETAILS_FILE} ]]; then
  		echo "$INFO creating CRT config file CRT_CSR_DETAILS_FILE $CRT_CSR_DETAILS_FILE"

    		cat > ${CRT_CSR_DETAILS_FILE} <<-EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${DNS_NAME}
EOF
  	else
  		echo "$INFO  CRT config file CRT_CSR_DETAILS_FILE $CRT_CSR_DETAILS_FILE exists, skipping"
  	fi

}

# Combines our CA w/ our CSR -- this signs our CRT using our CA_FILE
ensureSignedCrt () {
	echo "+ + + + + + + + + + + + + + + Ensuring Signed Cert $CRT_CERT_FILE + + + + + + + + + + + + + + + "
	if [ ! -f $CRT_CERT_FILE ]; then
  		echo "$INFO creating CRT_CERT_FILE $CRT_CERT_FILE"

      # we need to sign our crert w/ the CA private key 
	    ensureCA

	    # we need the cert signing request
      ensureCrtCR

      # we need to reference an 'extfile' for the config of this CSR
      ensureCrtCSRConfFile

  		openssl x509 -req -in ${CRT_CSR_FILE} -CA ${CA_FILE} -CAkey ${CA_PRIVATE_KEY_FILE} -passin file:$CA_PWFILE -CAcreateserial -out $CRT_CERT_FILE -days 1825 -sha256 -extfile $CRT_CSR_DETAILS_FILE
  	else
  		echo "$INFO Signed certificate CRT_CERT_FILE $CRT_CERT_FILE exists, skipping"
  	fi
}

# Converts the CRT_CERT_FILE into a .jks format
ensureJKSFromSignedCertificate () {
  echo "+ + + + + + + + + + + + + + + Ensuring JKS file $CRT_CERT_FILE_JKS + + + + + + + + + + + + + + + "
  if [[ ! -f ${CRT_CERT_FILE_JKS} ]]; then
      echo "$INFO creating CRT_CERT_FILE_JKS $CRT_CERT_FILE_JKS"

      ensureSignedCrt
      ensureCrtJKSPassword

      keytool -noprompt -importcert -alias ${CRT_CERT_FILE_JKS_ALIAS} -file ${CRT_CERT_FILE} -keystore $CRT_CERT_FILE_JKS -storepass ${CRT_JKS_PW}

      #echo "$INFO created jks file $CRT_CERT_FILE_JKS from  $CRT_CERT_FILE with alias $CRT_CERT_FILE_JKS_ALIAS :"
      #keytool -list -v -keystore $CRT_CERT_FILE_JKS -storepass ${CRT_JKS_PW}
    else
      echo "$INFO JKS file CRT_CERT_FILE_JKS $CRT_CERT_FILE_JKS exists, skipping"
    fi
}


# Converts the CRT_CERT_FILE into a .p12 format
ensureP12FromSignedCertificate () {
  echo "+ + + + + + + + + + + + + + + Ensuring .p12 file $CRT_CERT_FILE_P12 + + + + + + + + + + + + + + + "
  if [[ ! -f ${CRT_CERT_FILE_P12} ]]; then
      echo "$INFO creating CRT_CERT_FILE_P12 $CRT_CERT_FILE_P12"

      # ensure a password for this .p12 file
      ensureCRTPassword

      # we need our signed .crt file to convert
      ensureSignedCrt

      # https://www.ssl.com/how-to/create-a-pfx-p12-certificate-file-using-openssl/
      openssl pkcs12 -passout file:${CRT_PWFILE} -export -out ${CRT_CERT_FILE_P12} -inkey ${CRT_KEY_FILE} -in ${CRT_CERT_FILE}
    else
      echo "$INFO p12 file CRT_CERT_FILE_P12 $CRT_CERT_FILE_P12 exists, skipping"
    fi
}
