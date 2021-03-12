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