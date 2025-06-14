# Mini Stock Market

A mini stock market simulation designed to study and demonstrate the use of multithreading, with real-world technologies such as Spring Boot and OpenTelemetry.

## 💡 Overview

**MiniStockMarket** is a Java-based project that simulates a simplified stock exchange environment where multiple traders interact with the market.

This simulation is intended for educational and experimental purposes, focusing on concurrency and distributed systems concepts.

The project showcases how traders (threads) buy and sell stocks concurrently, providing insights into synchronization, thread management, and monitoring with modern observability tools.

## ✨ Features

- Simulated stock exchange environment;
- Multiple traders acting as concurrent threads;
- Buy/sell operations for stocks;
- Demonstrates synchronization and thread safety;
- Instrumentation with OpenTelemetry for monitoring and tracing;
- Built using Spring Boot for easy configuration and extensibility;

## 🛠️ Technologies Used

- **Java**: Core programming language for the application;
- **Spring Boot**: Framework for building and running the application;
- **OpenTelemetry**: For tracing, monitoring, and observability;
- **Multithreading**: Core concept demonstrated by the traders and exchange simulation;
- **Multimodule Project**: Organized into modules for better structure and maintainability;
- **Maven**: For dependency management and build automation;
- **SLF4J**: For logging and integration with otel;
- **Grafana**: For visualizing metrics and traces;
- **Spring Data JPA**: For database interactions;
- **RESTful APIs**: For exposing endpoints to interact with the stock market simulation;
- **Swagger/OpenAPI**: For API documentation and manual testing;
- **Github Actions**: For CI/CD pipeline to build and test the project;

## 👨🏻‍💻 Getting Started

### ✅ Prerequisites

- Java 24 or newer installed;
- Maven for dependency management;
- Docker for running the Grafana LGTM stack;

### 🚀 Running the Application

1. **Build the project:**
    - `mvn clean install`
2. **Run the application:**
    - Launch `StockExchangeApplication` from the `stock-exchange` module.
    - Launch `TraderApplication` from the `trader` module.
3. **Open Swagger UI** to explore the API endpoints:
    - Navigate to [Swagger UI](http://localhost:8080/swagger-ui/index.html) or the configured port.
4. **Open Grafana** to observe logs and traces:
    - in the [port 8080](http://localhost:8080) or at the configured port.

## 📈 Customization

- You can configure the number of traders, stocks, and other parameters by editing the application properties or source code.
- Extend or modify trader strategies to experiment with different behaviors and concurrency patterns.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
