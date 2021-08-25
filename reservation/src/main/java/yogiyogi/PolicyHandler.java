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
    @Autowired ReservationRepository reservationRepository;
    @Autowired CancellationRepository cancellationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_AcceptReserve(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener AcceptReserve : " + paymentApproved.toJson() + "\n\n");

     
        //System.out.println("### Listener : " + paymentApproved.toJon());
        Reservation reservation = new Reservation();
        reservation.setStatus("Reservation Complete");
        reservation.setOrderId(paymentApproved.getOrderId());
        reservation.setId(paymentApproved.getOrderId());
        reservationRepository.save(reservation);
        

        // Sample Logic //
        // Reservation reservation = new Reservation();
        // reservationRepository.save(reservation);
        // Cancellation cancellation = new Cancellation();
        // cancellationRepository.save(cancellation);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCanceled_CancelReserve(@Payload OrderCanceled orderCanceled){

        if(!orderCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelReserve : " + orderCanceled.toJson() + "\n\n");



        // Sample Logic //
        // Reservation reservation = new Reservation();
        // reservationRepository.save(reservation);
        Cancellation cancellation = new Cancellation();
        cancellation.setOrderId(orderCanceled.getId());
        cancellation.setStatus("Reservation Canceled");
        cancellationRepository.save(cancellation);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverConfirmCanceled_CancelReserve(@Payload ConfirmCanceled confirmCanceled){

        if(!confirmCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelReserve : " + confirmCanceled.toJson() + "\n\n");



        // Sample Logic //
        // Reservation reservation = new Reservation();
        // reservationRepository.save(reservation);
        Cancellation cancellation = new Cancellation();
        cancellation.setOrderId(confirmCanceled.getOrderid());
        cancellation.setStatus("Confirmation Canceled");
        cancellationRepository.save(cancellation);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
