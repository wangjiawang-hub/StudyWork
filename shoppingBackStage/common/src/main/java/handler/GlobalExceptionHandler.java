package handler;

import exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import model.Result;
import model.ResultCodeEnum;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // ① 业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.failure(e.getResultCode(), e.getMessage());
    }

    // ② 兜底
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.failure(ResultCodeEnum.SERVER_ERROR, "服务异常: " + e.getMessage());
    }
    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public Result<Void> handleDbConnectionException(CannotGetJdbcConnectionException e) {
        log.error("数据库连接失败，请检查数据库是否启动", e);
        return Result.failure(ResultCodeEnum.SERVER_ERROR, "数据库连接失败，请检查数据库服务");
    }
    @ExceptionHandler(MyBatisSystemException.class)
    public Result<Void> handleMyBatisException(MyBatisSystemException e) {
        Throwable cause = e.getCause();
        if (cause instanceof CannotGetJdbcConnectionException) {
            String msg = cause.getMessage() != null ? cause.getMessage() : "";
            if (msg.contains("Connection refused") || msg.contains("Communications link failure")) {
                log.warn("数据库未启动，无法连接: {}", msg);
                return Result.failure(ResultCodeEnum.SERVER_ERROR, "数据库服务未启动，请启动数据库");
            } else if (msg.contains("Access denied")) {
                log.warn("数据库密码错误，请检查配置: {}", msg);
                return Result.failure(ResultCodeEnum.SERVER_ERROR, "数据库密码错误，请检查配置");
            } else {
                log.warn("数据库连接失败: {}", msg);
                return Result.failure(ResultCodeEnum.SERVER_ERROR, "数据库连接失败");
            }
        }
        Throwable realCause = e.getCause() != null ? e.getCause().getCause() : null;
        if (realCause == null) realCause = e.getCause();
        log.warn("数据库连接失败: {}", realCause != null ? realCause.getMessage() : "未知原因");        return Result.failure(ResultCodeEnum.SERVER_ERROR, "数据库查询异常");
    }

}