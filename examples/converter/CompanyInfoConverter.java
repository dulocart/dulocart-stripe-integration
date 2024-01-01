@Component
public class CompanyInfoConverter {


    /***
     * Add toStripeInfo method in class
     * builder withAutoPaymentLink boolen info
     * If false payment link webhook need to be registered manual on Stripe service dashboard
     * regarding the controller path
     * @param companyInfo
     * @return
     */
    public StripeInfo toStripeInfo(CompanyInfo companyInfo) {
        return StripeInfo.builder()
                .taxId(companyInfo.getTaxId())
                .chargeCurrency(companyInfo.getChargeCurrency().name())
                .name(companyInfo.getName())
                .withAutoPaymentLink(false)
                .build();
    }

}