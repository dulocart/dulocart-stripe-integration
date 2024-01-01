@RestController
@RequestMapping(value = "/storefront/checkout")
public class CheckoutController {


    /***
     * Edit submitOrder enpoint as shown in this class
     * @param id
     * @return
     * @throws StripeException
     */
    @PatchMapping(value = "/{id}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable("id") Long id) throws StripeException {
        Checkout checkout = service.findById(id);
        Order order = orderConverter.toOrderFromCheckout(checkout);
        order.setDeliveryCost(checkout.getDelivery());
        orderService.save(order);

        //1. Convert order object to stripeOrder
        StripeOrder stripeOrder = orderConverter.toStripeOrder(order);
        //2. Create payment link for the order with stripeutility
        PaymentLink stripePaymentLink = stripeUtility.createStripePaymentLink(stripeOrder);
        //3. Set the payment link to the order
        order.setPaymentLinkId(stripePaymentLink.getId());
        //4. Update the order
        orderService.update(order, order.getId());
        decreaseProductQuantityWhenAddToOrder(order.getProducts(), productService);

        return ResponseEntity.ok(orderConverter.toOrderResponse(order));
    }
}