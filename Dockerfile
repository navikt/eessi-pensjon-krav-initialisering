FROM navikt/java:17-appdynamics

COPY build/libs/eessi-pensjon-krav-initialisering-0.0.1-SNAPSHOT.jar /app/app.jar

ENV APPD_NAME eessi-pensjon
ENV APPD_TIER krav-initialisering
ENV APPD_ENABLED true
