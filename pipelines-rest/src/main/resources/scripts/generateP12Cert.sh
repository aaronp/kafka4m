#!/usr/bin/env bash

THIS_DIR="$(dirname ${0})"
source ${THIS_DIR}/createCrt.sh

ensureP12FromSignedCertificate
