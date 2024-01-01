@Service
public class OrderServiceImpl implements OrderService {

    /***
     * Overide findByPaymentLink method from  OrderService -> StripeOrderService
     * @param id
     * @return
     */

    @Override
    public StripeOrder findByPaymentLink(String id) {
        return repository.findByPaymentLinkId(id).map(orderConverter::toStripeOrder)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Order with that payment link id %s does not exists", id)));
    }
}