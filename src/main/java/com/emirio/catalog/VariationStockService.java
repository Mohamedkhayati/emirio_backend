package com.emirio.catalog;

import com.emirio.catalog.repo.VariationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class VariationStockService {

    private final VariationRepository variations;
    private final StockAlertMailService stockAlertMailService;

    public VariationStockService(
        VariationRepository variations,
        StockAlertMailService stockAlertMailService
    ) {
        this.variations = variations;
        this.stockAlertMailService = stockAlertMailService;
    }

    public VariationArticle updateStock(Long variationId, int newStock) {
        if (newStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        VariationArticle variation = variations.findWithArticleAndVendeurById(variationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variation not found"));

        int oldStock = variation.getQuantiteStock();
        variation.setQuantiteStock(newStock);

        if (oldStock > 0 && newStock == 0 && !variation.isRuptureStockNotifEnvoyee()) {
            stockAlertMailService.sendOutOfStockAlert(variation, newStock);
            variation.setRuptureStockNotifEnvoyee(true);
        } else if (newStock > 0) {
            variation.setRuptureStockNotifEnvoyee(false);
        }

        return variations.save(variation);
    }

    public VariationArticle decreaseStock(Long variationId, int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        VariationArticle variation = variations.findWithArticleAndVendeurById(variationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variation not found"));

        int oldStock = variation.getQuantiteStock();

        if (quantity > oldStock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }

        int newStock = oldStock - quantity;
        variation.setQuantiteStock(newStock);

        if (oldStock > 0 && newStock == 0 && !variation.isRuptureStockNotifEnvoyee()) {
            stockAlertMailService.sendOutOfStockAlert(variation, newStock);
            variation.setRuptureStockNotifEnvoyee(true);
        } else if (newStock > 0) {
            variation.setRuptureStockNotifEnvoyee(false);
        }

        return variations.save(variation);
    }
}