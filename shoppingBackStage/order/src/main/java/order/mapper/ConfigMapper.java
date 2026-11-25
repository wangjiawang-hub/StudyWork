package order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConfigMapper {
    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key}")
    String getConfigValue(String key);
}
