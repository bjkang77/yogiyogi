package yogiyogi;

import yogiyogi.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverConfirmed_SendSms(@Payload Confirmed confirmed){

        if(!confirmed.validate()) return;

        System.out.println("\n\n##### listener SendSms : " + confirmed.toJson() + "\n\n");



        // Sample Logic //

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_SendSms(@Payload PaymentCanceled paymentCanceled){

        if(!paymentCanceled.validate()) return;

        System.out.println("\n\n##### listener SendSms : " + paymentCanceled.toJson() + "\n\n");



        // Sample Logic //

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
