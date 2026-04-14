package com.emirio.shop.repo;

import com.emirio.shop.model.ShopOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderItemRepository extends JpaRepository<ShopOrderItem, Long> {
}