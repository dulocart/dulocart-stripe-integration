/** Extend StripeCompanyInfoService
 * and add findFirstStripeInfo() method into existing CompanyInfoService
 */

public interface CompanyInfoService extends StripeCompanyInfoService {


    StripeInfo findFirstStripeInfo();
    ....
}