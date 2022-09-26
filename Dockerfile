FROM navikt/java:17

COPY init-scripts/ep-jvm-tuning.sh /init-scripts/

COPY build/libs/eessi-pensjon-krav-initialisering-0.0.1-SNAPSHOT.jar /app/app.jar
