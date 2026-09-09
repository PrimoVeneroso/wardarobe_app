#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p signing
if [[ -e signing/armadio-release.jks || -e signing/release.properties ]]; then
  echo 'Signing files already exist. Keep and reuse them; refusing to overwrite.' >&2
  exit 1
fi
signing_password=$(openssl rand -hex 32)
export ARMADIO_NEW_KEY_PASSWORD="$signing_password"
keytool -genkeypair -noprompt -keystore signing/armadio-release.jks \
  -storetype PKCS12 -alias armadio -keyalg RSA -keysize 3072 -validity 36500 \
  -storepass:env ARMADIO_NEW_KEY_PASSWORD -keypass:env ARMADIO_NEW_KEY_PASSWORD \
  -dname 'CN=Armadio, OU=Android, O=Armadio'
printf 'storeFile=../signing/armadio-release.jks\nstorePassword=%s\nkeyAlias=armadio\nkeyPassword=%s\n' \
  "$signing_password" "$signing_password" > signing/release.properties
unset ARMADIO_NEW_KEY_PASSWORD signing_password
echo 'Created signing/armadio-release.jks and signing/release.properties. Back up both privately.'
