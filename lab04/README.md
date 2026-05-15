# Lab 4: Separated Interface Pattern 

**Project:** City E-Scooter Rental - Multi-Provider Payment Processing System

This project demonstrates the **Separated Interface** pattern (as defined by Martin Fowler) using a multi-module Java application. 

## The Concept
An E-Scooter rental backend that calculates ride costs and processes payments. To keep the core domain completely independent of any infrastructure (databases or external APIs), we define the `RideRepository` and `PaymentGateway` interfaces inside the pure Java core, but implement them in separate infrastructure modules.

## Architecture & Modules
- `scooter-core`: Pure Java. Contains the business logic (`RideService`, `Ride`) and the separated interfaces (`RideRepository`, `PaymentGateway`). It has **zero dependencies**. 
- `scooter-postgres`: Infrastructure. Implements `RideRepository` using PostgreSQL and JDBC.
- `scooter-gateways`: Infrastructure. Implements `PaymentGateway` mocks like `StripeGateway` and `WalletGateway`.
- `scooter-app`: The Application layer. Wires the implementations to the core abstractions using Dependency Injection and runs a simulation.

## Prerequisites
- Java 17
- Maven
- Docker & Docker Compose (for PostgreSQL)

## How to Run

1. Start the PostgreSQL database:
   ```bash
   docker compose up -d
   ```

2. Compile and run the application simulation:
   ```bash
   mvn clean compile exec:java -pl scooter-app
   ```
