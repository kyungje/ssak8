package cleaningServiceydp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import cleaningServiceydp.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler{

	@Autowired
	private KakaoRepository kakaoRepository;
//    @StreamListener(KafkaProcessor.INPUT)
//    public void onStringEventListener(@Payload String eventString){
//
//    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCustomerRegisterCanceled_KakaoAlertCancel(@Payload CustomerRegisterCanceled customerRegisterCanceled){

        if(customerRegisterCanceled.isMe()){
            Kakao kakao = new Kakao();
            kakao.setCustomerName(customerRegisterCanceled.getCustomerName());
            kakao.setCustomerAddress(customerRegisterCanceled.getCustomerAddress());
            kakao.setCustomerAge(customerRegisterCanceled.getCustomerAge());

            kakaoRepository.save(kakao);
            System.out.println("##### listener KakaoAlertCancel : " + customerRegisterCanceled.toJson());
        }
    }
}
