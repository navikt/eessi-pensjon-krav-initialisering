##!/usr/bin/env bash

echo "Sjekker eessi-pensjon-krav-initialisering srvPassord"
if test -f /var/run/secrets/nais.io/srveessipensjonkrav/password;
then
  echo "Setter eessi-pensjon-krav-initialisering srvPassord"
    export srvpassword=$(cat /var/run/secrets/nais.io/srveessipensjonkrav/password)
fi

echo "Sjekker eessi-pensjon-krav-initialisering srvUsername"
if test -f /var/run/secrets/nais.io/srveessipensjonkrav/username;
then
    echo "Setter eessi-pensjon-krav-initialisering srvUsername"
    export srvusername=$(cat /var/run/secrets/nais.io/srveessipensjonkrav/username)
fi

echo "Sjekker eessi_pensjon_krav_s3_creds_password"
if test -f /var/run/secrets/nais.io/appcredentials/eessi_pensjon_krav_s3_creds_password;
then
  echo "Setter eessi_pensjon_krav_s3_creds_password"
    export eessi_pensjon_krav_s3_creds_password=$(cat /var/run/secrets/nais.io/appcredentials/eessi_pensjon_krav_s3_creds_password)
fi