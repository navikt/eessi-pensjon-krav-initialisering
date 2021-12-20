FROM navikt/java:11-appdynamics

COPY build/libs/eessi-pensjon-krav-initialisering-0.0.1-SNAPSHOT.jar /app/app.jar

COPY nais/export-vault-secrets.sh /init-scripts/

ENV APPD_NAME eessi-pensjon
ENV APPD_TIER krav-initialisering
ENV APPD_ENABLED true
