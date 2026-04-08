# Schriftliche Dokumentation Entwurf

## Einleitung

Das Projekt Boodle ist ein Backend für die Verwaltung von Benutzern, Kursen, Vorlesungen, Räumen, Kursgruppen und Stundenplaneinträgen. Eigentlich war auch noch ein Frontend geplant, aber das wäre viel zu viel Arbeit geworden für den Zeitrahmen. Deshalb habe ich den Fokus fast komplett auf das Backend gelegt. Das Backend ist fachlich schon sehr weit fertig. Eigentlich müsste man am Ende hauptsächlich noch ein Frontend bauen.

Die Architektur wurde früh auf einen containerisierten Betrieb ausgerichtet. Docker Compose, Dockerfiles und die GitHub Actions Pipeline sind Teil des Projekts, damit sich das Backend lokal und in CI möglichst gleich verhält. Die Secrets liegen aktuell noch in der `.env.example`, weil das den Start viel einfacher macht. Später müsste man das natürlich sauberer lösen und die Secrets wo anders speichern.

Für die Authentifizierung habe ich JWT genommen. Damit habe ich schon viel gute Erfahrung gemacht und es passt extrem gut zu einem stateless REST Backend. Das Backend speichert keine Session, sondern prüft bei jedem Request das Token und leitet daraus Benutzer, Rollen und Rechte ab. Für die Persistenz habe ich JetBrains Exposed genutzt, weil die Bibliothek minimaler ist, ziemlich modern wirkt und ich nicht viel SQL selber schreiben will.

## 1 Domain Driven Design

### 1.1 Analyse der Ubiquitous Language

Die Fachsprache vom Projekt ist bewusst nah am echten Anwendungsfall gehalten. Begriffe wie Benutzer, Dozent, Student, Kurs, Vorlesung, Raum, Kursgruppe, reguläre Stunde, unregelmäßige Stunde und Anmeldung tauchen überall auf, also in Code, DTOs, Controllern, Tests und API Endpunkten. Dadurch bleibt alles viel nachvollziehbarer. Besonders wichtig ist die Trennung zwischen Kurs und Vorlesung. Ein Kurs ist eher die organisatorische Einheit, während eine Vorlesung den eigentlichen Inhalt und den Dozenten beschreibt. Der Stundenplan arbeitet dann mit regulären und unregelmäßigen Terminen, weil das fachlich einfach Sinn macht.

Die Ubiquitous Language wurde nicht extra akademisch aufgeblasen, sondern an die Begriffe angepasst, die ich im Projekt wirklich brauche. Das ist für ein Praxisbackend viel sinnvoller, weil man die Domäne direkt an den Klassennamen und Schnittstellen sieht.

### 1.2 Verwendung taktischer Muster des DDD

**Entities.**
Die wichtigsten fachlichen Objekte werden als Entities modelliert. Dazu gehören `User`, `Kurs`, `Vorlesung`, `Raum`, `Kursgruppe`, `RegulaereStunde` und `UnregulaereStunde`. Sie haben alle eine eindeutige Identität und einen Lebenszyklus. Beziehungen wie Kurszuordnungen oder Einschreibungen sind auch extra modelliert, zum Beispiel über `KursInLectureTable`, `KursInGruppeTable`, `KursEnrollmentTable` und `LectureEnrollmentTable`.

**Value Objects.**
Ich setze eher pragmatisch auf einfache, unveränderliche Datenträger und Enums als auf ganz viele extra Spezialobjekte. Beispiele sind `UserRole`, `Wochentag` und `UnregulaerStatus`. Diese Typen tragen fachliche Bedeutung, ohne eine eigene Identität zu brauchen. Die DTOs sind auch unveränderlich und helfen dabei, Daten klar und ohne Seiteneffekte zu transportieren.

**Aggregates.**
Die Aggregate sind so geschnitten, dass Änderungen möglichst nur über einen fachlich passenden Root laufen. `Kurs` ist der zentrale Bezugspunkt für kursbezogene Sachen wie Kurssprecher, Vorlesungszuordnungen und Anmeldungen. `Vorlesung` ist der Bezugspunkt für Stundenplan und Termindaten. `Kursgruppe` ist eine eigene Einheit für die Gruppierung von Kursen. `Raum` steht für einen physischen Raum mit Kapazität. Dadurch werden fachliche Regeln nicht überall verteilt, sondern bleiben viel besser zusammen.

**Repositories.**
Eine klassische Repository Schicht mit eigenen Interfaces habe ich nicht extra gebaut. Das Projekt folgt hier eher einem pragmatischen Ansatz. Exposed übernimmt den direkten Zugriff auf die Daten, während die Services die fachlichen Regeln kapseln. Für die Größe vom Projekt ist das viel sinnvoller, weil eine extra Repository Abstraktion vor allem mehr Boilerplate machen würde.

**Domain Services.**
Die fachliche Logik liegt in den Services, zum Beispiel in `KursService`, `VorlesungService`, `RaumService`, `KursgruppeService`, `StundenplanService`, `KursEnrollmentService` und `LectureEnrollmentService`. Dort sitzen Validierung, Dublettenprüfung, Konfliktprüfung und Ownership Checks. Das ist fachlich korrekt, weil diese Regeln nicht in die HTTP Schicht gehören, sondern in den Kern vom System.

### 1.3 Analyse und Begründung der verwendeten Muster

Die Domäne ist im Kern mittelgroß und ziemlich überschaubar. Deshalb wurde bewusst kein überkomplexes DDD Modell mit extrem vielen Zwischenabstraktionen gebaut. Stattdessen habe ich auf eine klare Zuordnung von fachlichen Begriffen, Entity Modellen und Services geachtet. Das passt viel besser zum KISS Prinzip und spart Aufwand.

Ein weiterer Grund für diesen Aufbau ist Exposed. Die Bibliothek arbeitet sowieso schon sehr nah an den fachlichen Modellen. Ein stark abstrahiertes Repository Design würde hier eher stören als helfen. Darum habe ich Entity, Tabellen und Service Logik an den Stellen gebündelt, wo sie fachlich zusammengehören. So bleibt alles kompakt und trotzdem verständlich.

## 2 Clean Architecture

### 2.1 Schichtarchitektur planen und begründen

Die Architektur ist in mehrere klare Ebenen gegliedert. Ganz oben stehen die Controller als HTTP Schnittstelle. Darunter liegen die Services, die die fachliche Logik ausführen. Die Persistenz wird durch Exposed Tabellen und Entity Klassen abgebildet. Zusätzlich gibt es noch eine Konfigurationsschicht für Security, JWT und Datenbankintegration.

Die Schichtung ist bewusst so gewählt, dass die Web Schicht möglichst wenig Logik enthält. Controller nehmen Requests entgegen, rufen Services auf und liefern Responses zurück. Die Business Regeln bleiben in den Services. Dadurch werden die Klassen besser testbar und Änderungen an der API beeinflussen die Fachlogik nur indirekt.

### 2.2 Mindestens zwei Schichten umgesetzt

Im Projekt sind mindestens zwei Schichten klar umgesetzt. Das sind die Controller Schicht und die Service beziehungsweise Domain Schicht. In den Controllern liegen Request Mapping, HTTP Statuscodes und Fehlerantworten. In den Services liegen Validierung, Besitzprüfung, Konfliktprüfung und Datenzugriff. Das sieht man auch viel bei den Mock Tests, wo die Controller ohne echte Datenbank getestet werden.

Die Persistenzschicht ist ebenfalls klar getrennt. Tabellen und Entities liegen in den Model Dateien und werden über Exposed angesprochen. Das ist nicht als super strenge Clean Architecture gebaut, aber für die Größe vom Projekt ist es sauber genug und viel wartbarer als ein komplett durcheinanderes Setup.

## 3 Programming Principles

### 3.1 Single Responsibility Principle

Die Klassen haben meistens einen klaren Schwerpunkt. Controller kümmern sich um HTTP, Services um Fachlogik, `SecurityUtils` um den Zugriff auf den Security Context und die Exposed Modelle um die Persistenz. Dadurch gibt es viel weniger Vermischung von Aufgaben. Besonders deutlich ist das bei `StundenplanService`. Die Klasse prüft Ownership und Raumkonflikte, während der Controller nur Requests weiterreicht.

### 3.2 DRY

Wiederholte Logik wurde zusammengezogen. In `StundenplanService` sind gemeinsame Prüfungen in Hilfsmethoden wie `validateBasics` und `validateRoomChoice` ausgelagert. Auch die Ownership Prüfung wurde über `SecurityUtils` zentralisiert. Das reduziert doppelte Codefragmente und macht Änderungen einfacher, weil man eine Regel nur an einer Stelle anpassen muss.

### 3.3 KISS

Das Projekt ist bewusst einfach gehalten. Statt viele Zwischenebenen einzuführen, liegen verwandte Daten und Logik in gemeinsamen Model Dateien. Das spart viel Springerei im Code und macht Änderungen schneller nachvollziehbar. Auch die Entscheidung, das Frontend erstmal zu entfernen, passt zu KISS. Es wurde nicht versucht, ein viel zu großes Gesamtpaket zu erzwingen, sondern das Backend sauber fertigzustellen.

### 3.4 Low Coupling

Abhängigkeiten wurden möglichst klein gehalten. Controller kennen die Services, aber nicht die Datenbankdetails. Die Services greifen über Exposed auf die Daten zu, ohne dass die Web Schicht davon etwas wissen muss. In den Tests werden Controller durch MockK von ihren Services getrennt, damit man die Schichten einzeln testen kann.

### 3.5 High Cohesion

Zusammengehörige Funktionalität liegt jeweils dicht beieinander. Kurs bezogene Regeln finden sich im Kurs Modell und in den zugehörigen Services, Stundenplanregeln im Stundenplan Service, Raumregeln im Raum Service. Diese Bündelung erhöht die Kohäsion und senkt den Aufwand, wenn man eine fachliche Regel ändert.

## 4 Unit Tests

### 4.1 Umfang und Aufbau

Die Testsuite enthält deutlich mehr als die geforderten 10 Unit Tests. Allein die Mock Tests für die Controller decken 14 konkrete Testfälle ab. Dazu kommen noch viele Integrations und Fachtests für Persistenz, Validierung, Rollenverhalten, Raumkapazität und Zeitkonflikte. Insgesamt liegt die Projektbasis bei über 60 Testmethoden.

Die Unit Tests konzentrieren sich auf die HTTP Schicht. Dort werden die Services mit MockK ersetzt, damit man gezielt das Verhalten der Controller prüfen kann. Beispiele sind erfolgreiche und fehlerhafte Login Vorgänge, das Erzeugen von Kursen, das Suchen von Vorlesungen oder die Rückgabe von Konfliktantworten im Stundenplan Controller.

### 4.2 ATRIP-Regeln

**Automatic.** Die Tests laufen automatisch über JUnit und Gradle, ohne manuelle Vorbereitung.

**Thorough.** Erfolgsfälle und Fehlfälle werden beide abgedeckt. Beispiele sind gültige Eingaben, fehlende Benutzer, falsche Passwörter, Konflikte bei Raumbuchungen und ungültige Validierung.

**Repeatable.** Die Controller Tests sind durch Mocks isoliert. Die Integrations Tests arbeiten mit H2 im Speicher, sodass jeder Lauf reproduzierbar ist.

**Independent.** Die Tests bauen ihre Ausgangslage jeweils selbst auf und reinigen ihre Daten nach jedem Lauf. Dadurch sind sie nicht voneinander abhängig.

**Professional.** Die Tests sind fachlich benannt, halten sich an Arrange Act Assert und prüfen klare Erwartungen mit `assertEquals`, `assertThrows` und ähnlichen Assertions.

### 4.3 Einsatz von Mocks

MockK wird vor allem in den Controller Tests eingesetzt. Beispiele sind `UserService`, `KursService`, `StundenplanService`, `LectureEnrollmentService`, `KursEnrollmentService`, `JwtTokenService` und `PasswordEncoder`. Dadurch wird die obere Schicht ohne echte Datenbank und ohne echte Security Abhängigkeiten getestet. Das ist wichtig, weil dann wirklich nur die Verantwortung vom Controller geprüft wird.

## 5 Refactoring

### 5.1 Code Smells identifizieren

Zu Beginn gab es mehrere typische Code Smells, die in einem wachsenden Backend schnell entstehen. Zum Beispiel wiederholte Sicherheitsabfragen, gemischte Verantwortlichkeiten in Services, längere Methoden mit mehreren Validierungsschritten und eine zu starke Verteilung von zusammengehöriger Logik auf viele kleine Dateien. In Tests ist auch aufgefallen, dass die Bereinigung in der falschen Reihenfolge zu Foreign Key Problemen führen kann.

### 5.2 Mindestens zwei Refactorings anwenden

Das erste Refactoring war das Extrahieren von `SecurityUtils`. Vorher hätten Services den Security Context direkt auslesen müssen. Durch die Auslagerung ist der Zugriff auf die aktuelle Matrikelnummer jetzt viel zentraler, testbarer und lesbarer.

Das zweite Refactoring war die Zerlegung längerer Service Methoden in klar benannte Hilfsfunktionen. In `StundenplanService` wurden gemeinsame Schritte wie Grundvalidierung, Raumwahl und Konfliktprüfung getrennt. Dadurch sind die Methoden kürzer und die fachlichen Regeln besser nachvollziehbar.

Ein drittes sinnvolles Refactoring war die konsequente Nutzung von DTOs. Controller geben keine Entities direkt nach außen, sondern dedizierte Datenobjekte. Das entkoppelt die API von der internen Persistenzstruktur und reduziert das Risiko, aus Versehen Datenbankdetails preiszugeben.

### 5.3 Begründung der Refactorings

Die Refactorings verbessern Lesbarkeit, Testbarkeit und Wartbarkeit. Sie reduzieren Duplikate und sorgen dafür, dass ähnliche Regeln an einer Stelle gepflegt werden. Gerade bei Ownership Prüfungen, Zeitkonflikten und Validierungen ist das wichtig, weil sich solche Regeln sonst schnell in leicht unterschiedlicher Form wiederholen.

## 6 Entwurfsmuster

### 6.1 Eingesetzte Muster

Ein klar erkennbares Entwurfsmuster ist die Chain of Responsibility im Security Teil. Der `JwtAuthenticationFilter` ist in die Spring Security Filterkette eingebunden und verarbeitet jeden Request, bevor er an die Controller weitergeht. Erst wenn das Token erfolgreich geprüft wurde, wird eine Authentication im Security Context gesetzt. Damit ist die Authentifizierung nicht an die Controller gekoppelt, sondern Teil einer Kette von Verarbeitungsstufen.

Ein zweites Muster ist das Strategy Pattern. Für die Passwortprüfung wird das `PasswordEncoder` Interface genutzt, konkret mit `BCryptPasswordEncoder` als Implementierung. Dadurch bleibt der Authentifizierungsprozess austauschbar. Die Logik weiß nur, dass ein Passwort geprüft oder gehasht werden kann, nicht aber, wie genau die Implementierung intern arbeitet.

### 6.2 Einsatz begründen

Diese Muster passen gut zum Projekt, weil sie die Security Logik modular halten. Die Filterkette ist für JWT natürlich, da jeder Request denselben Prüfpfad durchläuft. Das Strategy Pattern ist ebenfalls sinnvoll, weil Passwortverarbeitung und spätere Algorithmen sauber über ein Interface austauschbar bleiben. Für ein Projekt dieser Größe sind das passende Muster ohne viel unnötige Komplexität.

## Projektentscheidungen und Grenzen

Die wichtigsten Entscheidungen waren bewusst pragmatisch. JWT wurde wegen guter eigener Erfahrung und wegen der stateless API gewählt. Exposed wurde genommen, um SQL möglichst klein zu halten und trotzdem eine moderne Kotlin Integration zu haben. Die Secrets liegen derzeit noch in `.env.example`, weil das den Entwicklungsstart viel einfacher macht. Später sollte man das in eine sichere Produktionslösung überführen.

Die Modellierung im Backend folgt dem KISS Prinzip. Deshalb liegen Entity, Table und Service bei verwandten Fachobjekten nah beieinander. Das ist für dieses Projekt viel sinnvoller als eine sehr zerstreute Schichtstruktur. Das Frontend wurde aus Scope Gründen zurückgestellt, damit das Backend technisch sauber abgeschlossen werden konnte.

## Kurzes Fazit

Das Backend ist fachlich und technisch in einer stabilen Form umgesetzt. Die Domäne ist klar benannt, die Schichten sind getrennt, Tests decken die kritischen Pfade ab und die Sicherheitslogik ist zentral geregelt. Für die schriftliche Dokumentation kann man daraus die geforderten Analysen und Begründungen direkt ableiten.