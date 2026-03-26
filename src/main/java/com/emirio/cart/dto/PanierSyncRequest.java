package com.emirio.cart.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PanierSyncRequest {
    @Valid
    private List<CartItemRequest> items = new ArrayList<>();
}