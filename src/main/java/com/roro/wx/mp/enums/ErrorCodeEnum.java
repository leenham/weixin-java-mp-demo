package com.roro.wx.mp.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    SUCCESS(90000000,"成功"),
    RECEIVED(90000001,"已接收到请求"),
    UNKNOWN_ERROR(90009999,"系统未知错误"),

    /*处理暗号图报错*/
    CIPHER_ILLEGAL_ANSWER(90010001,"暗号图答案格式异常"),
    CIPHER_ILLEGAL_PIC(90010002,"读取暗号图失败,请发送官方暗号图原图"),
    CIPHER_WITHOUT_QRCODE(90010003,"图片指定区域内未能检测出二维码"),
    CIPHER_WRONG_QRCODE(90010004,"检测出不属于暗号图的二维码"),
    NO_COMMIT_CIPHER(90010005,"请先上传暗号图,再输入答案"),
    RECENT_CIPHER_ERROR(90010006,"最近一次提交的暗号图异常,请上传官方原图后再输入答案"),
    CIPHER_RETRIVAL_ERROR(90010007,"检索暗号图时发生异常"),
    FAIL_UPDATE_CIPHER_POOL(90010008,"更新暗号池时发生异常"),
    UNABLE_RESOLVE_CIPHER(90010009,"无法处理该暗号图"),
    CIPHER_SMALL_SIZE(90010010,"图片尺寸太小,请发送官方暗号图原图"),
    CIPHER_UNKNOWN_ERROR(90010999, "处理暗号图时未知异常"),

    PLACE_HOLDER(99999999,"占位符");
    private int code;
    private String desc;
    ErrorCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
