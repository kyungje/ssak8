package cleaningServiceydp;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Customer_table")
public class Customer {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String customerName;
    private String customerAddress;
    private Long customerAge;

    @PostPersist
    public void onPostPersist(){
        CustomerRegistered customerRegistered = new CustomerRegistered();
        BeanUtils.copyProperties(this, customerRegistered);
        customerRegistered.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        cleaningServiceydp.external.Kakao kakao = new cleaningServiceydp.external.Kakao();
        // mappings goes here
        CustomerApplication.applicationContext.getBean(cleaningServiceydp.external.KakaoService.class)
            .kakaoAlert(kakao);


        CustomerRegisterCanceled customerRegisterCanceled = new CustomerRegisterCanceled();
        BeanUtils.copyProperties(this, customerRegisterCanceled);
        customerRegisterCanceled.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }
    public Long getCustomerAge() {
        return customerAge;
    }

    public void setCustomerAge(Long customerAge) {
        this.customerAge = customerAge;
    }




}
