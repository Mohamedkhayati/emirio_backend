package com.emirio.shop.service;

import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.shop.dto.CheckoutItemRequest;
import com.emirio.shop.dto.CheckoutRequest;
import com.emirio.shop.model.ShopOrder;
import com.emirio.shop.model.ShopOrderItem;
import com.emirio.shop.repo.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ShopOrderService {

    private final ShopOrderRepository orderRepository;
    private final VariationRepository variationRepository;

    @Transactional
    public ShopOrder checkout(CheckoutRequest req) {
        try {
            ShopOrder order = new ShopOrder();
            order.setCustomerName(req.getCustomerName());
            order.setCustomerEmail(req.getCustomerEmail());
            order.setCustomerPhone(req.getCustomerPhone());

            BigDecimal total = BigDecimal.ZERO;

            for (CheckoutItemRequest itemReq : req.getItems()) {
                VariationArticle variation = variationRepository.findById(itemReq.getVariationId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));

                int qty = itemReq.getQuantity();
                int stock = variation.getQuantiteStock();

                if (qty <= 0) {
                    throw new ResponseStatusException(BAD_REQUEST, "Invalid quantity");
                }

                if (stock < qty) {
                    throw new ResponseStatusException(CONFLICT, "Insufficient stock");
                }

                variation.setQuantiteStock(stock - qty);

                ShopOrderItem line = new ShopOrderItem();
                line.setOrder(order);
                line.setVariation(variation);
                line.setQuantity(qty);

                BigDecimal unitPrice = BigDecimal.valueOf(variation.getPrix());
                line.setUnitPrice(unitPrice);
                line.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));

                order.getItems().add(line);
                total = total.add(line.getLineTotal());
            }

            order.setTotalAmount(total);
            return orderRepository.save(order);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(CONFLICT, "Stock changed, refresh and try again");
        }
    }
}