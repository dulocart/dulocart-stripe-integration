package com.intuitionlabs.dulocartstripeintegration.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeOrderProduct {

    private String name;
    private double price;
    private double discountPrice;
    private long quantity;


    public double getPrice() {
        if (this.discountPrice > 0) {
            return this.discountPrice;
        } else {
            return this.price;
        }
    }
}
