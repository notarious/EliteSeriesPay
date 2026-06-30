# EliteSeriesPay

EliteSeriesPay is a local desktop-oriented web application for managing paid community memberships, payments, and project budgets.

The application was originally created for managing paid fan translation communities, but it is designed to work with any small membership-based project where participants make recurring or package payments.

## Features

### Project management

* Create, edit and delete projects
* Configure:

  * monthly subscription fee (RUB)
  * monthly subscription fee (EUR)
  * episode cost (RUB)

### Participant management

* Create, edit and archive participants
* Add participants to multiple projects
* Support two billing modes:

  * Subscription
  * Package

### Payments

* Record payments in:

  * RUB
  * EUR
  * USD
* Support payment sources:

  * VK Donut
  * Other
* Automatic VK Donut fee calculation (10%)
* Automatic currency conversion to RUB
* Fetch current USD/EUR exchange rates
* Manual exchange-rate override
* Payment edit and void history

### Subscription billing

* Monthly subscription tracking
* Automatic paid-until calculation
* Partial payment handling
* Overdue detection
* Package participants without subscription tracking

### Reports

* Project payment history
* Participant payment history
* Monthly financial reports
* Funding progress
* Remaining amount until the next funded episode

### Data safety

* Automatic SQLite database backups
* Manual backup creation
* Backup management

## Technology Stack

* Java 21
* Spring Boot
* Spring Data JPA
* Thymeleaf
* Bootstrap 5
* SQLite
* Flyway
* Maven
* JUnit

## Architecture

The application follows a layered architecture.

* Controllers contain HTTP and navigation logic only.
* Business rules are implemented in the service layer.
* Repositories are responsible for persistence and database-level filtering.
* Thymeleaf templates contain presentation logic only.

Financial calculations, subscription billing, reporting, exchange-rate handling, and backup management are implemented as separate application services.

## Running the application

Requirements:

* Java 21
* Maven

Run:

```bash
mvn spring-boot:run
```

The application will be available at:

```
http://localhost:8080
```

The SQLite database is created automatically on first start.

## Windows installation

When installed via the MSI installer:

- application files: `C:\Program Files\EliteSeriesPay\`
- user data: `%LOCALAPPDATA%\EliteSeriesPay\`
- database: `%LOCALAPPDATA%\EliteSeriesPay\data\eliteseriespay.db`
- backups: `%LOCALAPPDATA%\EliteSeriesPay\backups\`
- logs: `%LOCALAPPDATA%\EliteSeriesPay\logs\`

User data is preserved across upgrades and uninstall. Only application files in `C:\Program Files\EliteSeriesPay\` are replaced during an upgrade.

See `BUILD_WINDOWS.md` for MSI build instructions and `README-RU.md` for end-user documentation in Russian.

## Tests

Run all tests:

```bash
mvn test
```

## Project goals

The project demonstrates:

* clean layered architecture
* service-oriented business logic
* repository-level filtering and pagination
* domain-driven business rules
* comprehensive unit and integration testing
* maintainable Spring Boot application structure

This application is intended both as a practical tool and as a portfolio project demonstrating backend engineering practices.
