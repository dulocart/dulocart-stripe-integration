package com.intuitionlabs.dulocartstripeintegration.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeOrder {

    private Long orderId;
    private List<StripeOrderProduct> products;
    private double deliveryCost;


}

