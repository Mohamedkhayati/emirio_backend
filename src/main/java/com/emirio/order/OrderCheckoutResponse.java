package com.emirio.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCheckoutResponse {
    private Long id;
    private String referenceCommande;
    private String statutCommande;
    private String statutPaiement;
    private String modePaiement;
    private String invoiceNumber;
    private String invoiceUrl;
    private String paymentInstructions;
}