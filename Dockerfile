FROM navikt/java:11-appdynamics

COPY build/libs/eessi-pensjon-behandle-utland-*.jar /app/app.jar
COPY build/libs/eessi-pensjon-behandleutland-*.jar /app/app.jar

COPY nais/export-vault-secrets.sh /init-scripts/

ENV APPD_NAME eessi-pensjon
ENV APPD_TIER behandle-utland
ENV APPD_ENABLED true
