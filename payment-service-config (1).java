// Configuration files for Payment Service

// 1. application.yml
// src/main/resources/application.yml
```yaml
server:
  port: 8082
  servlet:
    context-path: /

spring:
  application:
    name: payment-service
  
  # Database Configuration
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_payment
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA/Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  # Jackson Configuration
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
    default-property-inclusion: non_null

# Logging Configuration
logging:
  level:
    root: INFO
    com.ecommerce.payment: DEBUG
    org.springframework.web: INFO
    org.hibernate: INFO
```

// 2. Dockerfile
// Dockerfile
```
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
```

// 3. pom.xml
// pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                            https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    
    <groupId>com.ecommerce</groupId>
    <artifactId>payment-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>payment-service</name>
    <description>Payment Service for E-Commerce Application</description>
    
    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.1.0</version>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
    
</project>
```

// 4. API Documentation Configuration
// OpenApiConfig.java
package com.ecommerce.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("RESTful API for E-Commerce Payment Processing")
                        .version("1.0")
                        .contact(new Contact()
                                .name("E-Commerce Team")
                                .email("team@ecommerce.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.ecommerce.com/payment")
                                .description("Production Server")));
    }
}

// 5. WebMvc Configuration (for CORS)
// WebConfig.java
package com.ecommerce.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}

// 6. Payment Gateway Integration (simulated)
// PaymentGatewayService.java
package com.ecommerce.payment.gateway;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.exception.PaymentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentGatewayService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);
    
    /**
     * Process payment through external payment gateway
     * This is a simulated implementation
     *
     * @param paymentRequest the payment request details
     * @return transaction ID from payment gateway
     * @throws PaymentProcessingException if payment processing fails
     */
    public String processPayment(PaymentRequest paymentRequest) throws PaymentProcessingException {
        logger.info("Processing payment through payment gateway for order: {}", paymentRequest.getOrderId());
        
        try {
            // Simulate API call to payment gateway
            // In a real implementation, this would make an HTTP call to a payment provider API
            
            // Simulate processing time
            Thread.sleep(1000);
            
            // Simulate random success/failure (90% success rate)
            if (Math.random() < 0.9) {
                String transactionId = "PG-" + UUID.randomUUID().toString();
                logger.info("Payment processed successfully. Transaction ID: {}", transactionId);
                return transactionId;
            } else {
                logger.error("Payment gateway declined the payment for order: {}", paymentRequest.getOrderId());
                throw new PaymentProcessingException("Payment declined by payment gateway");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Payment processing interrupted", e);
            throw new PaymentProcessingException("Payment processing was interrupted");
        } catch (Exception e) {
            logger.error("Error occurred while processing payment", e);
            throw new PaymentProcessingException("Payment gateway error: " + e.getMessage());
        }
    }
    
    /**
     * Process a refund through external payment gateway
     * This is a simulated implementation
     *
     * @param transactionId original transaction ID to refund
     * @return refund transaction ID from payment gateway
     * @throws PaymentProcessingException if refund processing fails
     */
    public String processRefund(String transactionId) throws PaymentProcessingException {
        logger.info("Processing refund through payment gateway for transaction: {}", transactionId);
        
        try {
            // Simulate API call to payment gateway for refund
            
            // Simulate processing time
            Thread.sleep(800);
            
            // Simulate random success/failure (95% success rate for refunds)
            if (Math.random() < 0.95) {
                String refundTransactionId = "RF-" + UUID.randomUUID().toString();
                logger.info("Refund processed successfully. Refund Transaction ID: {}", refundTransactionId);
                return refundTransactionId;
            } else {
                logger.error("Payment gateway declined the refund for transaction: {}", transactionId);
                throw new PaymentProcessingException("Refund declined by payment gateway");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Refund processing interrupted", e);
            throw new PaymentProcessingException("Refund processing was interrupted");
        } catch (Exception e) {
            logger.error("Error occurred while processing refund", e);
            throw new PaymentProcessingException("Payment gateway error during refund: " + e.getMessage());
        }
    }
}

// 7. Payment Notification Service
// PaymentNotificationService.java
package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending notifications about payment events
 * In a real implementation, this would integrate with an email or messaging service
 */
@Service
public class PaymentNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationService.class);
    
    /**
     * Send notification about successful payment
     *
     * @param paymentResponse the payment details
     */
    public void sendPaymentConfirmation(PaymentResponse paymentResponse) {
        logger.info("Sending payment confirmation for order: {}, transaction: {}", 
            paymentResponse.getOrderId(), paymentResponse.getTransactionId());
        
        // In a real implementation, this would:
        // 1. Call an email service to send confirmation to customer
        // 2. Potentially notify other services (order service, etc.)
    }
    
    /**
     * Send notification about failed payment
     *
     * @param orderId the order ID
     * @param reason the failure reason
     */
    public void sendPaymentFailureNotification(Long orderId, String reason) {
        logger.info("Sending payment failure notification for order: {}, reason: {}", orderId, reason);
        
        // In a real implementation, this would:
        // 1. Call an email service to notify customer about the failed payment
        // 2. Notify other relevant services
    }
    
    /**
     * Send notification about payment refund
     *
     * @param paymentResponse the payment details
     */
    public void sendRefundConfirmation(PaymentResponse paymentResponse) {
        logger.info("Sending refund confirmation for order: {}, transaction: {}", 
            paymentResponse.getOrderId(), paymentResponse.getTransactionId());
        
        // In a real implementation, this would:
        // 1. Call an email service to send refund confirmation to customer
        // 2. Notify other relevant services
    }
}

// 8. Integration with Order Service
// OrderServiceClient.java
package com.ecommerce.payment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with the Order Service
 * Used to update order status after payment processing
 */
@Component
public class OrderServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    
    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${service.order.url:http://localhost:8081/api/orders}") String orderServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
    }
    
    /**
     * Update order status after successful payment
     *
     * @param orderId the order ID
     * @param paymentId the payment ID
     */
    public void updateOrderAfterPayment(Long orderId, Long paymentId) {
        logger.info("Updating order status for orderId: {} after payment: {}", orderId, paymentId);
        
        try {
            // In a real implementation, this would make an API call to the Order Service
            // For now, we'll just log the action
            
            // Example of what the real call might look like:
            // OrderStatusUpdateRequest request = new OrderStatusUpdateRequest("PAID", paymentId);
            // restTemplate.postForEntity(orderServiceUrl + "/" + orderId + "/status", request, Void.class);
            
            logger.info("Order status updated successfully");
        } catch (Exception e) {
            logger.error("Failed to update order status: {}", e.getMessage());
            // In a production environment, consider implementing a retry mechanism or placing
            // the update request in a queue for later processing
        }
    }
    
    /**
     * Update order status after refund
     *
     * @param orderId the order ID
     * @param paymentId the payment ID
     */
    public void updateOrderAfterRefund(Long orderId, Long paymentId) {
        logger.info("Updating order status for orderId: {} after refund: {}", orderId, paymentId);
        
        try {
            // In a real implementation, this would make an API call to the Order Service
            // For now, we'll just log the action
            
            // Example of what the real call might look like:
            // OrderStatusUpdateRequest request = new OrderStatusUpdateRequest("REFUNDED", paymentId);
            // restTemplate.postForEntity(orderServiceUrl + "/" + orderId + "/status", request, Void.class);
            
            logger.info("Order status updated successfully after refund");
        } catch (Exception e) {
            logger.error("Failed to update order status after refund: {}", e.getMessage());
            // In a production environment, consider implementing a retry mechanism
        }
    }
}

// 9. RestTemplate Configuration
// RestTemplateConfig.java
package com.ecommerce.payment.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
    }
}

// 10. Data Masking Utility for PCI Compliance
// PaymentDataMasker.java
package com.ecommerce.payment.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for masking sensitive payment data
 * Important for PCI compliance and security
 */
@Component
public class PaymentDataMasker {
    
    /**
     * Mask a credit card number, showing only the last 4 digits
     * 
     * @param cardNumber the full card number
     * @return masked card number (e.g., "************1234")
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        
        int visibleDigits = 4;
        int length = cardNumber.length();
        
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < length - visibleDigits; i++) {
            masked.append("*");
        }
        
        masked.append(cardNumber.substring(length - visibleDigits));
        return masked.toString();
    }
    
    /**
     * Mask CVV completely
     * 
     * @param cvv the CVV code
     * @return masked CVV (always "***")
     */
    public String maskCvv(String cvv) {
        return "***";
    }
    
    /**
     * Partially mask an email address
     * 
     * @param email the full email address
     * @return partially masked email (e.g., "j***@example.com")
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****@****.com";
        }
        
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        
        if (name.length() <= 1) {
            return name + "***@" + domain;
        }
        
        return name.charAt(0) + "***@" + domain;
    }
}