package yogiyogi;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="HotelView_table")
public class HotelView {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long orderid;
        private Long reservationId;
        private Long cancellationId;
        private String name;
        private String status;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public Long getOrderid() {
            return orderid;
        }

        public void setOrderid(Long orderid) {
            this.orderid = orderid;
        }
        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }
        public Long getCancellationId() {
            return cancellationId;
        }

        public void setCancellationId(Long cancellationId) {
            this.cancellationId = cancellationId;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

}