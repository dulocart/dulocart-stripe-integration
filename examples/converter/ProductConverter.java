@Component
public class ProductConverter {


    /**
     * Add toStripeOrderProduct method in class
     * @param product
     * @return
     */
    public StripeOrderProduct toStripeOrderProduct(OrderProduct product) {
        StripeOrderProduct build = StripeOrderProduct.builder()
                .quantity(product.getQuantity())
                .price(product.getPrice())
                .name(product.getName())
                .discountPrice(product.getDiscountPrice())
                .build();

        return build;
    }

}