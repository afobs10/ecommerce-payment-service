package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.client.OrderServiceClient;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.gateway.PaymentGatewayService;
import com.ecommerce.payment.model.Payment;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentNotificationService;
import com.ecommerce.payment.service.PaymentService;
import com.ecommerce.payment.util.PaymentDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the Payment Service with integration to payment gateway
 * and other services for a complete payment flow
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentNotificationService notificationService;
    private final OrderServiceClient orderServiceClient;
    private final PaymentDataMasker dataMasker;
    
    @Autowired
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentGatewayService paymentGatewayService,
            PaymentNotificationService notificationService,
            OrderServiceClient orderServiceClient,
            PaymentDataMasker dataMasker) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayService = paymentGatewayService;
        this.notificationService = notificationService;
        this.orderServiceClient = orderServiceClient;
        this.dataMasker = dataMasker;
    }
    
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment for order: {}, amount: {}, method: {}", 
            paymentRequest.getOrderId(),
            paymentRequest.getAmount(),
            paymentRequest.getPaymentMethod());
        
        // Log masked sensitive data for debugging (PCI compliant)
        if (paymentRequest.getCardNumber() != null) {
            logger.debug("Processing with card: {}", dataMasker.maskCardNumber(paymentRequest.getCardNumber()));
        }
        
        // Create a new payment record
        Payment payment = new Payment(
            paymentRequest.getOrderId(),
            paymentRequest.getAmount(),
            paymentRequest.getPaymentMethod()
        );
        
        // Set payment status to PROCESSING
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);
        
        // Process payment through gateway
        try {
            // Call payment gateway
            String transactionId = paymentGatewayService.processPayment(paymentRequest);
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            
            // Save the updated payment
            payment = paymentRepository.save(payment);
            
            // Send confirmation notification
            PaymentResponse response = convertToPaymentResponse(payment);
            notificationService.sendPaymentConfirmation(response);
            
            // Update order status
            orderServiceClient.updateOrderAfterPayment(paymentRequest.getOrderId(), payment.getId());
            
            logger.info("Payment completed successfully for order: {}", paymentRequest.getOrderId());
            return response;
            
        } catch (PaymentProcessingException e) {
            // Payment failed at gateway level
            logger.error("Payment processing failed for order: {}", paymentRequest.getOrderId(), e);
            
            // Update payment status to FAILED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            
            // Send failure notification
            notificationService.sendPaymentFailureNotification(
                paymentRequest.getOrderId(), 
                e.getMessage()
            );
            
            throw e;
        }
    }
    
    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        logger.info("Retrieving payment with ID: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> {
                logger.warn("Payment not found with ID: {}", paymentId);
                return new PaymentNotFoundException("Payment not found with ID: " + paymentId);
            });
        
        return convertToPaymentResponse(payment);
    }
    
    @Override
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        logger.info("Retrieving payments for order ID: {}", orderId);
        
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        logger.info("Found {} payments for order ID: {}", payments.size(), orderId);
        
        return payments.stream()
            .map(this::convertToPaymentResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public PaymentResponse refundPayment(Long paymentId) {
        logger.info("Processing refund for payment ID: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> {
                logger.warn("Payment not found for refund with ID: {}", paymentId);
                return new PaymentNotFoundException("Payment not found with ID: " + paymentId);
            });
        
        // Check if payment can be refunded
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            logger.warn("Cannot refund payment with status: {}", payment.getStatus());
            throw new PaymentProcessingException(
                "Only completed payments can be refunded. Current status: " + payment.getStatus());
        }
        
        try {
            // Process refund through payment gateway
            paymentGatewayService.processRefund(payment.getTransactionId());
            
            // Update payment status
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            
            // Create response
            PaymentResponse response = convertToPaymentResponse(payment);
            
            // Send refund confirmation
            notificationService.sendRefundConfirmation(response);
            
            // Update order status
            orderServiceClient.updateOrderAfterRefund(payment.getOrderId(), payment.getId());
            
            logger.info("Refund processed successfully for payment ID: {}", paymentId);
            return response;
            
        } catch (Exception e) {
            logger.error("Refund processing failed for payment ID: {}", paymentId, e);
            throw new PaymentProcessingException("Failed to process refund: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void deletePayment(Long paymentId) {
        logger.info("Deleting payment with ID: {}", paymentId);
        
        if (!paymentRepository.existsById(paymentId)) {
            logger.warn("Cannot delete - payment not found with ID: {}", paymentId);
            throw new PaymentNotFoundException("Payment not found with ID: " + paymentId);
        }
        
        paymentRepository.deleteById(paymentId);
        logger.info("Payment deleted successfully with ID: {}", paymentId);
    }
    
    /**
     * Helper method to convert Payment entity to PaymentResponse DTO
     * 
     * @param payment the payment entity
     * @return the payment response DTO
     */
    private PaymentResponse convertToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setAmount(payment.getAmount());
        response.setTransactionId(payment.getTransactionId());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        
        return response;
    }
}
