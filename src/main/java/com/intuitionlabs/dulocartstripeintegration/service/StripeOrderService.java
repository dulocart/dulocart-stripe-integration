package com.intuitionlabs.dulocartstripeintegration.service;


import com.intuitionlabs.dulocartstripeintegration.model.StripeOrder;

public interface StripeOrderService {
    StripeOrder findByPaymentLink(String id);

}
