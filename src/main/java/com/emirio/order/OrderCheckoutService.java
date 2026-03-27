package com.emirio.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private final InvoiceService invoiceService;

    public String generateInvoiceUrl(Commande commande) {
        return invoiceService.generateProformaInvoice(commande);
    }
}