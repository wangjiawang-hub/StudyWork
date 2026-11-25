package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    /** 仅允许写入（注册/创建用户），响应中不返回密码 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private Integer role;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private String token;
}
