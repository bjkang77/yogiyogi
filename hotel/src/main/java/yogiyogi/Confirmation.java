package yogiyogi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Confirmation_table")
public class Confirmation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderid;
    private String status;

    @PostPersist
    public void onPostPersist(){

        if ( this.getStatus().equals("Confirmation Cancel"))
        {
            ConfirmCanceled confirmCanceled = new ConfirmCanceled();
            BeanUtils.copyProperties(this, confirmCanceled);
            confirmCanceled.setEventType("ConfirmCanceled");
            //confirmCanceled.setOrderid(this.getId());
            confirmCanceled.publishAfterCommit();
        }
        else
        {
            Confirmed confirmed = new Confirmed();
            BeanUtils.copyProperties(this, confirmed);
            confirmed.publishAfterCommit();
        }
        

    }
    //@PostUpdate
    //public void onPostUpdate(){
    //    ConfirmCanceled confirmCanceled = new ConfirmCanceled();
    //    BeanUtils.copyProperties(this, confirmCanceled);
    //    confirmCanceled.setEventType("ConfirmCanceled");
    //    confirmCanceled.setOrderid(this.getId());
    //    confirmCanceled.publishAfterCommit();

    //}
    @PrePersist
    public void onPrePersist(){
    }

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
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}