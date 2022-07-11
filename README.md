![](https://github.com/navikt/eessi-pensjon-krav-initialisering/workflows/Bygg%20og%20deploy%20Q2/badge.svg)
![](https://github.com/navikt/eessi-pensjon-krav-initialisering/workflows/Manuell%20deploy/badge.svg)

# eessi-pensjon-krav-initialisering
Opprette krav-initialisering

# Utvikling

Kjør med java 11.05 minimum

## Komme i gang

Dette prosjektet bygger med avhengigheter som ligger i Github Package Registry.
Du må opprette et Personal Access Token (PAT) og enten legge det i 
`~/.gradle/gradle.properties`:
```properties
gpr.key=<ditt-token-her>
```
eller sette miljøvariabelen `GITHUB_TOKEN` til verdien av tokenet ditt.

Deretter kan du bygge med:
```
./gradlew build
```

## Oppdatere avhengigheter

Det er viktig at man holder avhengigheter oppdatert for å unngå sikkerhetshull.

Se mer dokumentasjon rundt dette her: [Oppgradere avhengigheter](https://github.com/navikt/eessi-pensjon/blob/master/docs/dev/oppgradere_avhengigheter.md).

## SonarQube m/JaCoCo

Prosjektet er satt opp med støtte for å kunne kjøre SonarQube, med JaCoCo for å fange test coverage, men du trenger å ha en SonarQube-instans (lokal?) å kjøre dataene inn i - [les mer her](https://github.com/navikt/eessi-pensjon/blob/master/docs/dev/sonarqube.md).
