# E-Commerce Payment Service

This repository contains the implementation of the Payment Service for the E-Commerce application.

## Overview

The Payment Service is responsible for processing payments, managing refunds, and tracking payment history for the e-commerce platform. It follows a RESTful API design and is part of a microservices architecture.

## Features

- Process payments using different payment methods (credit card, PayPal, etc.)
- Retrieve payment information by ID
- Retrieve all payments for a specific order
- Process refunds
- Integration with payment gateways (simulated)
- Integration with other services (Order Service)
- Notification system for payment events

## Technology Stack

- **Framework**: Spring Boot 3.1.5
- **Database**: MySQL (configurable, can use H2 for development)
- **Build Tool**: Maven
- **Java Version**: 17
- **Documentation**: OpenAPI (Swagger)

## Project Structure

```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/payment/
│   │   │   ├── client/            # Service clients for external communication
│   │   │   ├── config/            # Configuration classes
│   │   │   ├── controller/        # REST API controllers
│   │   │   ├── dto/               # Data Transfer Objects
│   │   │   ├── exception/         # Custom exceptions and error handlers
│   │   │   ├── gateway/           # Payment gateway integration
│   │   │   ├── model/             # Entity classes
│   │   │   ├── repository/        # Data access layer
│   │   │   ├── service/           # Business logic
│   │   │   │   └── impl/          # Service implementations
│   │   │   └── util/              # Utility classes
│   │   └── resources/
│   │       ├── application.yml    # Application configuration
│   │       └── static/            # Static resources (HTML forms)
│   └── test/                      # Test classes
├── Dockerfile                     # Docker configuration
└── pom.xml                        # Maven configuration
```

## API Endpoints

| Method | Endpoint                       | Description                               |
|--------|--------------------------------|-------------------------------------------|
| POST   | /api/payments                  | Process a new payment                     |
| GET    | /api/payments/{id}             | Get payment by ID                         |
| GET    | /api/payments/order/{orderId}  | Get all payments for a specific order     |
| POST   | /api/payments/{id}/refund      | Process a refund for a payment            |
| DELETE | /api/payments/{id}             | Delete a payment (admin functionality)    |

## REST Principles Implementation

The Payment Service follows REST principles:

1. **Resource-Based**: The API is designed around resources (payments) with clear URIs
2. **HTTP Methods**: Uses standard HTTP methods (GET, POST, DELETE) for operations
3. **Stateless**: Each request contains all the information needed to complete the request
4. **JSON Format**: Uses JSON for data exchange
5. **HATEOAS**: Includes links to related resources (partially implemented)
6. **Status Codes**: Uses appropriate HTTP status codes for responses
7. **Idempotent Operations**: GET and DELETE operations are idempotent

## Quality Attributes

### Security
- Data masking for sensitive payment information (PCI compliance)
- Input validation
- Exception handling to prevent information leakage

### Performance
- Connection pooling for database access
- Timeout configurations for external service calls

### Scalability
- Stateless design enables horizontal scaling
- Cloud-native ready for deployment on cloud platforms

### Reliability
- Comprehensive error handling
- Transaction management
- Logging for troubleshooting

## Setup and Running

### Prerequisites
- Java 17+
- Maven
- MySQL (or use H2 for development)

### Configuration
Edit `application.yml` to configure:
- Database connection
- Server port
- Logging settings

### Build and Run

```bash
# Clone the repository
git clone <repository-url>

# Navigate to the project directory
cd payment-service

# Build the project
mvn clean package

# Run the application
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

### Running with Docker

```bash
# Build Docker image
docker build -t ecommerce/payment-service .

# Run Docker container
docker run -p 8082:8082 ecommerce/payment-service
```

## Testing

The service includes both unit tests and integration tests:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceImplTest
```

### Testing with Postman
A Postman collection is available to test the API endpoints.

## Front-End Integration

Simple HTML forms are provided for testing:
- `/payment-form.html` - Process a new payment
- `/payment-lookup.html` - Look up payment details
- `/payment-refund.html` - Process a refund

## Cloud Deployment

The service is designed to be deployed to a cloud platform (e.g., AWS, GCP, Azure)(AWS suggested by the professor):
- Dockerfile for containerization
- Stateless design for horizontal scaling
- Configuration externalization

## Future Enhancements

- Implement authentication and authorization
- Add more payment methods
- Implement real payment gateway integrations
- Add metrics and monitoring
- Implement circuit breaker pattern for external service calls
- Message queue integration for asynchronous processing
