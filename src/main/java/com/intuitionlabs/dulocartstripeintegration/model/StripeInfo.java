package com.intuitionlabs.dulocartstripeintegration.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class StripeInfo {

    private String name;
    private String chargeCurrency;
    private String taxId;
    private boolean withAutoPaymentLink;
}
