@RestController
@RequestMapping(value = "/storefront/orders")
public class StoreOrderController {

    /***
     * Add the following endpoint to the class
     * @param sigHeader
     * @param payload
     * @return
     * @throws StripeException
     * @throws IOException
     */


    //1. Webhook endpoint for stripe service, auto registred if toStripeInfo converter method has withAutoPaymentLink else should be registred from Stripe Dashboard
    @PostMapping(value = "/stripe-webhook")
    public ResponseEntity<HttpStatus> getOrderAfterPayment(@RequestHeader("Stripe-Signature") String sigHeader, @RequestBody String payload) throws StripeException, IOException {

        return getWebhookResult(sigHeader, payload, paymentEmitters, service);
    }

    //2. Emmiter subscription to listen for order update in the getWebhookResult() method
    @GetMapping("/subscribe-stripe-payment")
    public SseEmitter subscribeToStripePayment(@RequestParam Long orderId) throws IOException {
        SseEmitter emitter = new SseEmitter(-1L);
        connectToEmitter(orderId, emitter);

        paymentEmitters.put(orderId, emitter);

        return emitter;
    }

    //3. Unsubscribe emmiter after payment is done or exipred payment link
    @GetMapping("/unsubscribe-stripe-payment")
    public ResponseEntity<HttpStatus> unsubscribeFromPayment(@RequestParam Long orderId) {
        SseEmitter emitter = paymentEmitters.remove(orderId);
        if (emitter != null) {
            emitter.complete();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}