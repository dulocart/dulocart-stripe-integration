@Service
public class CompanyInfoServiceImpl implements CompanyInfoService {

    private final CompanyInfoRepository repository;

    //1. Add CompanyInfoConverter into existing implementation
    private final CompanyInfoConverter converter;

    public CompanyInfoServiceImpl(CompanyInfoRepository repository, CompanyInfoConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }


    /**
     * Override findFirstStripeInfo
     * @return
     */
    @Override
    public StripeInfo findFirstStripeInfo() {
        return repository.findFirstByOrderByIdAsc().map(converter::toStripeInfo)
                .orElseThrow(() -> new ResourceNotFoundException("Company info does not exists. Please create company info."));
    }

}