package yogiyogi;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelViewRepository extends CrudRepository<HotelView, Long> {

    List<HotelView> findByOrderid(Long orderid);
}