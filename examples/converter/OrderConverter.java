@Component
public class OrderConverter {

    private final ProductConverter productConverter;

    public OrderConverter(ProductConverter productConverter) {
        this.productConverter = productConverter;
    }

    /**
     * Add toStripeOrder method in class
     * @param order
     * @return
     */
    public StripeOrder toStripeOrder(Order order) {
        StripeOrder build = StripeOrder.builder()
                .orderId(order.getId())
                .deliveryCost(order.getDeliveryCost())
                .products(order.getProducts().stream()
                        .map(productConverter::toStripeOrderProduct).collect(Collectors.toList()))
                .build();

        return build;
    }

}