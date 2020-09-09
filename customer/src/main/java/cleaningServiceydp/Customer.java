package cleaningServiceydp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

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
    	cleaningServiceydp.external.Kakao kakao = new cleaningServiceydp.external.Kakao();

    	kakao.setCustomerName(getCustomerName());
    	kakao.setCustomerAddress(getCustomerAddress());
    	kakao.setCustomerAge(getCustomerAge());

        try {

        	CustomerApplication.applicationContext.getBean(cleaningServiceydp.external.KakaoService.class)
            	.kakaoAlert(kakao);
        }
        catch(Exception e) {
        	throw new RuntimeException("kakaoService failed. Check your kakao Service.");
        }
    }

    @PostRemove
    public void onPostRemove(){
    	CustomerRegisterCanceled customerRegisterCanceled = new CustomerRegisterCanceled();

    	customerRegisterCanceled.setCustomerAddress(getCustomerAddress());
    	customerRegisterCanceled.setCustomerName(getCustomerName());
    	customerRegisterCanceled.setCustomerAge(getCustomerAge());

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
