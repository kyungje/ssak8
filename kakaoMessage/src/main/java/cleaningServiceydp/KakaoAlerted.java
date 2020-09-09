package cleaningServiceydp;

public class KakaoAlerted extends AbstractEvent {

    private Long id;
    private String customerAddress;
    private String customerName;
    private Long customerAge;

    public KakaoAlerted(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public Long getCustomerAge() {
        return customerAge;
    }

    public void setCustomerAge(Long customerAge) {
        this.customerAge = customerAge;
    }
}
