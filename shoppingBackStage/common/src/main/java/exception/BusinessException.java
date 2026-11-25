package exception;

import lombok.Getter;
import model.ResultCodeEnum;

@Getter
public class BusinessException extends RuntimeException {
    private final ResultCodeEnum resultCode;

    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }
    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.resultCode = resultCode;

    }
}
