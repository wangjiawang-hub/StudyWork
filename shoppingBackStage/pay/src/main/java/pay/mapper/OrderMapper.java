package pay.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {
    @Update("UPDATE `order` SET pay_status=1, order_status=1, pay_time=NOW() WHERE order_no=#{orderNo}")
    int markPaid(String orderNo);
}
