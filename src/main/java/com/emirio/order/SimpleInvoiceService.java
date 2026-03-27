package com.emirio.order;

import org.springframework.stereotype.Service;

@Service
public class SimpleInvoiceService implements InvoiceService {

    @Override
    public String generateProformaInvoice(Commande commande) {
        return "/api/orders/" + commande.getId() + "/invoice";
    }
}