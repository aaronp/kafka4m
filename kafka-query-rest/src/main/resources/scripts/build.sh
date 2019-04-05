#!/usr/bin/env bash
source ./createCrt.sh

# ensureCA


ensureJKSFromSignedCertificate
ensureP12FromSignedCertificate
