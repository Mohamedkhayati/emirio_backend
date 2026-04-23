package com.emirio.chatbot.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopDetails {
    private String shopName;
    private String founded;
    private String founder;
    private String speciality;
    private String phone;
    private String email;
    private String facebook;
    private String addressDetails; // full address text
}