package com.intuitionlabs.dulocartstripeintegration.model;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StripePaymentLinkResponse {

    private HttpStatus status;
    private Long orderId;
    private String message;

}
