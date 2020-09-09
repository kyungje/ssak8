package cleaningServiceydp;

import cleaningServiceydp.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerViewViewHandler {


    @Autowired
    private CustomerViewRepository customerViewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCustomerRegistered_then_CREATE_1 (@Payload CustomerRegistered customerRegistered) {
        try {
            if (customerRegistered.isMe()) {
                // view 객체 생성
                CustomerView customerView = new CustomerView();
                // view 객체에 이벤트의 Value 를 set 함
                customerView.setId(customerRegistered.getId());
                customerView.setCustomerName(customerRegistered.getCustomerName());
                customerView.setCustomerAddress(customerRegistered.getCustomerAddress());
                customerView.setCustomerAge(customerRegistered.getCustomerAge());
                // view 레파지 토리에 save
                customerViewRepository.save(customerView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



}