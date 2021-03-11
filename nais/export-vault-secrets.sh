##!/usr/bin/env bash

echo "Sjekker eessi-pensjon-statistikk srvPassord"
if test -f /var/run/secrets/nais.io/srveessipensjonbeh/password;
then
  echo "Setter eessi-pensjon-statistikk srvPassord"
    export srvpassword=$(cat /var/run/secrets/nais.io/srveessipensjonbeh/password)
fi

echo "Sjekker eessi-pensjon-statistikk srvUsername"
if test -f /var/run/secrets/nais.io/srveessipensjonbeh/username;
then
    echo "Setter eessi-pensjon-statistikk srvUsername"
    export srvusername=$(cat /var/run/secrets/nais.io/srveessipensjonbeh/username)
fi