package yogiyogi;

import yogiyogi.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class HotelViewViewHandler {


    @Autowired
    private HotelViewRepository hotelViewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {
        try {

            if (!ordered.validate()) return;

            // view 객체 생성
            HotelView hotelView = new HotelView();
            // view 객체에 이벤트의 Value 를 set 함
            hotelView.setOrderid(ordered.getId());
            hotelView.setName(ordered.getName());
            hotelView.setStatus(ordered.getStatus());
            // view 레파지 토리에 save
            hotelViewRepository.save(hotelView);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_UPDATE_1(@Payload PaymentApproved paymentApproved) {
        try {
            if (!paymentApproved.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(paymentApproved.getOrderId());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(paymentApproved.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenReserveAccepted_then_UPDATE_2(@Payload ReserveAccepted reserveAccepted) {
        try {
            if (!reserveAccepted.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(reserveAccepted.getOrderId());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(reserveAccepted.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrderCanceled_then_UPDATE_3(@Payload OrderCanceled orderCanceled) {
        try {
            if (!orderCanceled.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(orderCanceled.getId());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(orderCanceled.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenReserveCanceled_then_UPDATE_4(@Payload ReserveCanceled reserveCanceled) {
        try {
            if (!reserveCanceled.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(reserveCanceled.getOrderId());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(reserveCanceled.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentCanceled_then_UPDATE_5(@Payload PaymentCanceled paymentCanceled) {
        try {
            if (!paymentCanceled.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(paymentCanceled.getOrderId());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(paymentCanceled.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenConfirmed_then_UPDATE_6(@Payload Confirmed confirmed) {
        try {
            if (!confirmed.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(confirmed.getOrderid());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(confirmed.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenConfirmCanceled_then_UPDATE_7(@Payload ConfirmCanceled confirmCanceled) {
        try {
            if (!confirmCanceled.validate()) return;
                // view 객체 조회

                    List<HotelView> hotelViewList = hotelViewRepository.findByOrderid(confirmCanceled.getOrderid());
                    for(HotelView hotelView : hotelViewList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    hotelView.setStatus(confirmCanceled.getStatus());
                // view 레파지 토리에 save
                hotelViewRepository.save(hotelView);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

