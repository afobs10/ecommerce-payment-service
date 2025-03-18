// Test classes for the Payment Service

// 1. Controller Tests
// PaymentControllerTest.java
package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.model.PaymentMethod;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setAmount(new BigDecimal("99.99"));
        paymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentRequest.setCardNumber("4111111111111111");
        paymentRequest.setCardHolderName("John Doe");
        paymentRequest.setExpiryDate("12/25");
        paymentRequest.setCvv("123");

        paymentResponse = new PaymentResponse();
        paymentResponse.setId(1L);
        paymentResponse.setOrderId(1L);
        paymentResponse.setAmount(new BigDecimal("99.99"));
        paymentResponse.setTransactionId("tx-123456789");
        paymentResponse.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentResponse.setStatus(PaymentStatus.COMPLETED);
        paymentResponse.setCreatedAt(LocalDateTime.now());
        paymentResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void processPayment_ShouldReturnCreatedStatus() throws Exception {
        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(99.99))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(paymentService, times(1)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void getPaymentById_ShouldReturnPayment() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(paymentResponse);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionId").value("tx-123456789"));

        verify(paymentService, times(1)).getPaymentById(1L);
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnPaymentsList() throws Exception {
        List<PaymentResponse> payments = Arrays.asList(paymentResponse);
        when(paymentService.getPaymentsByOrderId(1L)).thenReturn(payments);

        mockMvc.perform(get("/api/payments/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(paymentService, times(1)).getPaymentsByOrderId(1L);
    }

    @Test
    void refundPayment_ShouldReturnRefundedPayment() throws Exception {
        PaymentResponse refundedPayment = new PaymentResponse();
        refundedPayment.setId(1L);
        refundedPayment.setStatus(PaymentStatus.REFUNDED);
        
        when(paymentService.refundPayment(1L)).thenReturn(refundedPayment);

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(paymentService, times(1)).refundPayment(1L);
    }

    @Test
    void deletePayment_ShouldReturnNoContent() throws Exception {
        doNothing().when(paymentService).deletePayment(1L);

        mockMvc.perform(delete("/api/payments/1"))
                .andExpect(status().isNoContent());

        verify(paymentService, times(1)).deletePayment(1L);
    }
}

// 2. Service Tests
// PaymentServiceImplTest.java
package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentMethod;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        // Setup test data
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setAmount(new BigDecimal("99.99"));
        paymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentRequest.setCardNumber("4111111111111111");
        paymentRequest.setCardHolderName("John Doe");
        paymentRequest.setExpiryDate("12/25");
        paymentRequest.setCvv("123");

        payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(1L);
        payment.setAmount(new BigDecimal("99.99"));
        payment.setTransactionId("tx-123456789");
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void processPayment_ShouldReturnPaymentResponse() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void getPaymentById_ShouldReturnPaymentResponse() {
        // Arrange
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        // Act
        PaymentResponse response = paymentService.getPaymentById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("tx-123456789", response.getTransactionId());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void getPaymentById_ShouldThrowException_WhenPaymentNotFound() {
        // Arrange
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(99L));
        verify(paymentRepository, times(1)).findById(99L);
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnPaymentList() {
        // Arrange
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByOrderId(1L)).thenReturn(payments);

        // Act
        List<PaymentResponse> responses = paymentService.getPaymentsByOrderId(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void refundPayment_ShouldReturnRefundedPayment() {
        // Arrange
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // Act
        PaymentResponse response = paymentService.refundPayment(1L);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentNotCompleted() {
        // Arrange
        Payment pendingPayment = new Payment();
        pendingPayment.setId(1L);
        pendingPayment.setStatus(PaymentStatus.PENDING);
        
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

        // Act & Assert
        assertThrows(PaymentProcessingException.class, () -> paymentService.refundPayment(1L));
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void deletePayment_ShouldDeleteSuccessfully() {
        // Arrange
        when(paymentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(paymentRepository).deleteById(1L);

        // Act
        paymentService.deletePayment(1L);

        // Assert
        verify(paymentRepository, times(1)).existsById(1L);
        verify(paymentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletePayment_ShouldThrowException_WhenPaymentNotFound() {
        // Arrange
        when(paymentRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> paymentService.deletePayment(99L));
        verify(paymentRepository, times(1)).existsById(99L);
        verify(paymentRepository, never()).deleteById(any());
    }
}