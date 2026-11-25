package pay.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.easysdk.factory.Factory;
import model.AliPay;
import model.Result;
import model.ResultCodeEnum;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pay.config.AliPayConfig;
import pay.mapper.OrderMapper;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("alipay")
@Transactional(rollbackFor = Exception.class)
/** 支付宝沙箱：pagePay 返回 HTML 表单；syncPay/notify 验签后 markPaid 更新订单 */
public class AliPayController {
    @Resource
    private AliPayConfig aliPayConfig;
    @Resource
    private OrderMapper orderMapper;

    private static final String FORMAT = "JSON";
    private static final String CHARSET = "utf-8";
    private static final String SIGN_TYPE = "RSA2";

    private static final Set<String> RETURN_PARAM_KEYS = new HashSet<String>() {{
        add("charset");
        add("out_trade_no");
        add("method");
        add("total_amount");
        add("sign");
        add("trade_no");
        add("auth_app_id");
        add("version");
        add("app_id");
        add("sign_type");
        add("seller_id");
        add("timestamp");
    }};

    /**
     * 支付完成后同步订单：优先用浏览器回跳参数验签；失败则调支付宝查单接口。
     * 沙箱 504 时提示用户稍后在订单页「同步支付状态」。
     */
    @PostMapping("/syncPay")
    public Result<String> syncPay(@RequestBody(required = false) Map<String, String> body) {
        Map<String, String> params = body != null ? body : new HashMap<>();
        String traceNo = params.get("out_trade_no");
        if (!StringUtils.hasText(traceNo)) {
            traceNo = params.get("traceNo");
        }
        if (!StringUtils.hasText(traceNo)) {
            return Result.failure(ResultCodeEnum.FAIL, "订单号不能为空");
        }

        if (StringUtils.hasText(params.get("sign"))) {
            Result<String> returnResult = confirmByReturnParams(filterReturnParams(params));
            if (Boolean.TRUE.equals(returnResult.getSuccess())) {
                return returnResult;
            }
        }

        try {
            String tradeStatus = queryTradeStatus(traceNo);
            if (isPaidStatus(tradeStatus)) {
                return markPaidResult(traceNo);
            }
            return Result.failure(ResultCodeEnum.FAIL, "支付宝交易未完成，当前状态: " + tradeStatus);
        } catch (Exception e) {
            e.printStackTrace();
            if (StringUtils.hasText(params.get("sign"))) {
                return Result.failure(ResultCodeEnum.FAIL,
                        "支付宝沙箱查单超时，且回跳验签未通过。请稍后在订单页点击「同步支付状态」，或等待异步通知更新。");
            }
            if (isGatewayUnavailable(e)) {
                return Result.failure(ResultCodeEnum.FAIL,
                        "支付宝沙箱网关繁忙（504/502），请稍后点击「同步支付状态」重试，或等待异步通知更新订单。");
            }
            return Result.failure(ResultCodeEnum.FAIL, "查询支付宝交易失败: " + e.getMessage());
        }
    }

    private Result<String> confirmByReturnParams(Map<String, String> params) {
        if (!params.containsKey("sign") || !StringUtils.hasText(params.get("out_trade_no"))) {
            return Result.failure(ResultCodeEnum.FAIL, "缺少支付回跳参数");
        }
        try {
            if (Factory.Payment.Common().verifyNotify(params)) {
                if (!StringUtils.hasText(params.get("trade_no"))) {
                    return Result.failure(ResultCodeEnum.FAIL, "支付回跳缺少支付宝交易号");
                }
                return markPaidResult(params.get("out_trade_no"));
            }
            return Result.failure(ResultCodeEnum.FAIL, "支付回跳验签失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure(ResultCodeEnum.FAIL, "支付回跳验签异常: " + e.getMessage());
        }
    }

    private Map<String, String> filterReturnParams(Map<String, String> raw) {
        Map<String, String> filtered = new HashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            if (RETURN_PARAM_KEYS.contains(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private Result<String> markPaidResult(String orderNo) {
        if (orderMapper.markPaid(orderNo) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "订单不存在或已支付");
        }
        return Result.success("支付状态已同步");
    }

    private boolean isPaidStatus(String status) {
        return "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
    }

    private boolean isGatewayUnavailable(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("504")
                || lower.contains("502")
                || lower.contains("gateway time-out")
                || lower.contains("gateway timeout")
                || lower.contains("<!doctype html");
    }

    private String queryTradeStatus(String outTradeNo) throws AlipayApiException {
        AlipayClient alipayClient = buildAlipayClient();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (!response.isSuccess()) {
            throw new AlipayApiException(response.getSubMsg() != null ? response.getSubMsg() : response.getMsg());
        }
        return response.getTradeStatus();
    }

    private AlipayClient buildAlipayClient() {
        return new DefaultAlipayClient(
                aliPayConfig.resolveGatewayFullUrl(),
                aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(),
                FORMAT, CHARSET,
                aliPayConfig.getAlipayPublicKey(),
                SIGN_TYPE);
    }

    /** 生成电脑网站支付表单 HTML，前端 window.open + document.write 打开 */
    @PostMapping("/pay")
    public Result<String> pay(AliPay aliPay) {
        if (aliPay == null || !StringUtils.hasText(aliPay.getTraceNo())) {
            return Result.failure(ResultCodeEnum.FAIL, "订单号不能为空");
        }
        try {
            AlipayClient alipayClient = buildAlipayClient();
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(aliPayConfig.getNotifyUrl());
            if (StringUtils.hasText(aliPayConfig.getReturnUrl())) {
                request.setReturnUrl(aliPayConfig.getReturnUrl());
            }
            request.setBizContent("{\"out_trade_no\":\"" + aliPay.getTraceNo() + "\","
                    + "\"total_amount\":\"" + aliPay.getTotalAmount() + "\","
                    + "\"subject\":\"" + aliPay.getSubject() + "\","
                    + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
            String form = alipayClient.pageExecute(request).getBody();
            return Result.success(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return Result.failure(ResultCodeEnum.FAIL, "支付宝沙箱调用失败: " + e.getMessage());
        }
    }

    /** 支付宝服务器异步通知，用户未回跳浏览器时仍可更新订单 */
    @PostMapping("/notify")
    public String payNotify(HttpServletRequest request) {
        String tradeStatus = request.getParameter("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        System.out.println("=========支付宝异步回调========");
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }
        try {
            if (Factory.Payment.Common().verifyNotify(params)) {
                String tradeNo = params.get("out_trade_no");
                System.out.println("商户订单号: " + tradeNo);
                orderMapper.markPaid(tradeNo);
            } else {
                System.err.println("支付宝异步回调验签失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }
}
