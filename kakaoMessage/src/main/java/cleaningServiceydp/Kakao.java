package cleaningServiceydp;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Kakao_table")
public class Kakao {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String customerAddress;
    private String customerName;
    private Long customerAge;

    @PostPersist
    public void onPostPersist(){
        KakaoAlerted kakaoAlerted = new KakaoAlerted();
        BeanUtils.copyProperties(this, kakaoAlerted);
        kakaoAlerted.publishAfterCommit();


        KakaoAlertedCanceled kakaoAlertedCanceled = new KakaoAlertedCanceled();
        BeanUtils.copyProperties(this, kakaoAlertedCanceled);
        kakaoAlertedCanceled.publishAfterCommit();


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
