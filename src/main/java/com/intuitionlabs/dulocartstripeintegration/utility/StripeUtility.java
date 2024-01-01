package com.intuitionlabs.dulocartstripeintegration.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.intuitionlabs.dulocartstripeintegration.model.StripeInfo;
import com.intuitionlabs.dulocartstripeintegration.model.StripeOrder;
import com.intuitionlabs.dulocartstripeintegration.model.StripeOrderProduct;
import com.intuitionlabs.dulocartstripeintegration.model.StripePaymentLinkResponse;
import com.intuitionlabs.dulocartstripeintegration.service.StripeCompanyInfoService;
import com.intuitionlabs.dulocartstripeintegration.service.StripeOrderService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class StripeUtility {

    private final static String STRIPE_API_KEY = System.getenv("STRIPE_API_KEY");
    private final static String DELIVERY_CHARGE = "Delivery coast";
    private String baseUrl;
    private static Logger logger = LoggerFactory.getLogger(StripeUtility.class);

    @Autowired
    private final StripeCompanyInfoService companyInfoService;
    @Autowired
    private final Environment environment;

    public StripeUtility(Environment environment, StripeCompanyInfoService companyInfoService) throws StripeException {
        this.environment = environment;
        this.companyInfoService = companyInfoService;
        this.baseUrl = environment.getProperty("app.base-url");

        Stripe.apiKey = STRIPE_API_KEY;
        if(getCompanyInfo().isWithAutoPaymentLink()) createPaymentLinkWebhook();
    }


    public PaymentLink createStripePaymentLink(StripeOrder order) throws StripeException {
        List<Price> prices = getAllPrices(order);
        PaymentLinkCreateParams.LineItem deliveryCost = createLineItemDeliveryCost(order);

        List<PaymentLinkCreateParams.LineItem> lineItems = packLineItems(prices, order.getProducts());
        lineItems.add(deliveryCost);
        PaymentLinkCreateParams params = PaymentLinkCreateParams.builder()
                .addAllLineItem(lineItems)
                .setInvoiceCreation(createInvoice())
                .build();

        return PaymentLink.create(params);
    }

    @Async
    public static ResponseEntity<HttpStatus> getWebhookResult(String sigHeader, String payload, Map<Long, SseEmitter> emitters, StripeOrderService orderService) throws StripeException, IOException {
        String endpointSecret = System.getenv("STRIPE_PAYMENT_LINK_SECRET"); //
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (JsonSyntaxException e) {
            logger.debug("Invalid payload");

            return ResponseEntity.badRequest().build();
        } catch (SignatureVerificationException e) {
            logger.debug("Signature verification exception");

            return ResponseEntity.badRequest().build();
        }

        switch (event.getType()) {
            case "checkout.session.expired":
                String paymentLinkId = getPaymentLinkId(event);
                logger.info(String.format("Payment session expired for payment link id %s", paymentLinkId));

                executeEmitter(emitters, HttpStatus.REQUEST_TIMEOUT, orderService, paymentLinkId);
                break;
            case "checkout.session.completed":
                paymentLinkId = getPaymentLinkId(event);
                updatePaymentLinkAsCompleted(paymentLinkId);
                logger.info(String.format("Payment completed and checkout session completed for payment link id %s", paymentLinkId));

                executeEmitter(emitters, HttpStatus.CREATED, orderService, paymentLinkId);
                break;
            default:
                logger.info("Unhandled event type: " + event.getType());
        }

        return ResponseEntity.ok().build();
    }

    public static void updatePaymentLinkAsCompleted(String linkId) throws StripeException {
        PaymentLink paymentLink = PaymentLink.retrieve(linkId);
        PaymentLinkUpdateParams params = PaymentLinkUpdateParams.builder().setActive(false).build();

        paymentLink.update(params);
    }


    private static Product createProduct(StripeOrderProduct orderProduct) throws StripeException {
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(orderProduct.getName())
                .build();

        return Product.create(params);
    }

    private Price createPriceForProduct(StripeOrderProduct orderProduct, Product product) throws StripeException {
        PriceCreateParams params = PriceCreateParams.builder()
                .setProduct(product.getId())
                .setCurrency(getCompanyInfo().getChargeCurrency().toLowerCase())
                .setUnitAmount(calculateAmountFromOrderTotal(orderProduct.getPrice()))
                .build();

        return Price.create(params);
    }

    private List<Price> getAllPrices(StripeOrder order) {
        return order.getProducts().stream().map(x -> {
            try {
                return createPriceForProduct(x, createProduct(x));
            } catch (StripeException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private List<PaymentLinkCreateParams.LineItem> packLineItems(List<Price> prices, List<StripeOrderProduct> orderProducts) {
        return prices.stream().map(x -> PaymentLinkCreateParams.LineItem.builder()
                        .setPrice(x.getId())
                        .setQuantity(orderProducts.get(prices.indexOf(x)).getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private PaymentLinkCreateParams.LineItem createLineItemDeliveryCost(StripeOrder order) throws StripeException {
        StripeOrderProduct deliveryProduct = StripeOrderProduct.builder().name(DELIVERY_CHARGE).price(order.getDeliveryCost()).build();
        Price price = createPriceForProduct(deliveryProduct, createProduct(deliveryProduct));

        return PaymentLinkCreateParams.LineItem.builder()
                .setPrice(price.getId())
                .setQuantity(1L)
                .build();
    }

    private PaymentLinkCreateParams.InvoiceCreation createInvoice() {
        return PaymentLinkCreateParams.InvoiceCreation.builder()
                .setEnabled(true)
                .setInvoiceData(getInvoiceData())
                .build();
    }


    private PaymentLinkCreateParams.InvoiceCreation.InvoiceData getInvoiceData() {
        return PaymentLinkCreateParams.InvoiceCreation.InvoiceData.builder()
//                .addAccountTaxId(getCompanyInfo().getTaxId())
                .setDescription(String.format("Invoice for your order from %s", getCompanyInfo().getName()))
                .setFooter("Thank you for shopping with us.")
                .build();
    }

    private WebhookEndpoint createPaymentLinkWebhook() throws StripeException {
        String paymentLinkWebHookUrl = String.format("%s/%s/%s", baseUrl, "api/v1/storefront/orders", "stripe-webhook");

        WebhookEndpointCreateParams params =
                WebhookEndpointCreateParams.builder()
                        .setUrl(paymentLinkWebHookUrl)
                        .addAllEnabledEvent(Arrays.asList(
                                WebhookEndpointCreateParams.EnabledEvent.CHECKOUT__SESSION__COMPLETED,
                                WebhookEndpointCreateParams.EnabledEvent.CHARGE__FAILED,
                                WebhookEndpointCreateParams.EnabledEvent.CHECKOUT__SESSION__EXPIRED))
                        .build();

        return WebhookEndpoint.create(params);
    }

    private static String getPaymentLinkId(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            System.out.println("Deserializer is failing");
        }

        Session session = (Session) stripeObject;
        return session.getPaymentLink();
    }


    private static long calculateAmountFromOrderTotal(double total) {
        return (long) (total * 100);
    }

    private StripeInfo getCompanyInfo() {
        return companyInfoService.findFirstStripeInfo();
    }


    private static void executeEmitter(Map<Long, SseEmitter> emitters, HttpStatus code, StripeOrderService orderService, String paymentLinkId) throws IOException {
        StripeOrder order = orderService.findByPaymentLink(paymentLinkId);
        SseEmitter emitter = emitters.get(order.getOrderId());

        StripePaymentLinkResponse data = StripePaymentLinkResponse.builder()
                .status(code)
                .orderId(order.getOrderId())
                .message(code.equals(HttpStatus.CREATED) ? "Payment succeeded" : "Payment link expired")
                .build();

        emitter.send(SseEmitter.event().name("Notification").data(new ObjectMapper().writeValueAsString(data)));
    }

    public static void connectToEmitter(Long orderId, SseEmitter emitter) throws IOException {
        StripePaymentLinkResponse data = StripePaymentLinkResponse.builder()
                .status(HttpStatus.OK)
                .orderId(orderId)
                .message("Connected to payment emitter")
                .build();

        emitter.send(SseEmitter.event().name("Info").data(new ObjectMapper().writeValueAsString(data)));
    }
}
