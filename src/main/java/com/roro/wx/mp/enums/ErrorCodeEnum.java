package com.roro.wx.mp.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    SUCCESS(900000,"成功"),
    RECEIVED(900001,"已接收到请求"),
    UNHANDLED(900002,"无法处理该请求"),
    NO_AUTH(900003,"操作权限不足,请联系开发者."),
    ENDUE_AUTH_FAIL(900004,"授权失败"),
    USER_UNEXISTED(900005,"该用户不存在"),
    SPECIAL_COMMAND_ERROR(900006,"处理特殊指令时发生未知异常"),
    COMMAND_FORMAT_ERROR(900007,"指令格式错误导致异常"),
    UNKNOWN_ERROR(900099,"系统未知错误"),

    /*处理暗号图报错*/
    CIPHER_ILLEGAL_ANSWER(900101,"暗号图答案格式异常"),
    CIPHER_ILLEGAL_PIC(900102,"读取暗号图失败,请发送官方暗号图原图"),
    CIPHER_WITHOUT_QRCODE(900103,"图片指定区域内未能检测出二维码"),
    CIPHER_WRONG_QRCODE(900104,"检测出不属于暗号图的二维码"),
    NO_COMMIT_CIPHER(900105,"请先上传暗号图,再输入答案"),
    RECENT_CIPHER_ERROR(900106,"最近一次提交的暗号图异常,请上传官方原图后再输入答案"),
    CIPHER_RETRIVAL_ERROR(900107,"检索暗号图时发生异常"),
    FAIL_UPDATE_CIPHER_POOL(900108,"更新暗号池时发生异常"),
    UNABLE_RESOLVE_CIPHER(900109,"无法处理该暗号图"),
    CIPHER_SMALL_SIZE(900110,"图片尺寸太小,请发送官方暗号图原图"),
    CIPHER_UNKNOWN_ERROR(900199, "处理暗号图时未知异常"),

    /*处理答题报错*/
    NO_QUIZ_FOUND(900201,"未找到匹配项,请尝试更换检索词"),
    QUIZ_ILLEGAL_UPDATE(900202,"更新题库时发生异常,请检查输入格式"),
    QUIZ_WRONG_LABEL(900203,"题目编号格式错误,请检查输入格式"),
    QUIZ_UNEXIST_LABEL(900204,"当前编号不存在,请修改后重新输入"),
    NO_RECENT_COMMIT_QUIZ(900205,"没能检测到最近检索的单条记录,不允许修改"),
    QUIZ_WRONG_COMMAND(900206,"修改题目的指令格式异常"),
    QUIZ_WRONG_CHOICE(900207,"修改选项的指令格式异常"),
    QUIZ_UNKNOWN_ERROR(900299,"答题助手发生未知异常"),

    PLACE_HOLDER(999999,"占位符");
    private int code;
    private String desc;
    ErrorCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public boolean equals(ErrorCodeEnum ecode){
        return this.getCode()==ecode.getCode();
    }
    public boolean hashCode(ErrorCodeEnum ecode){
        return this.getCode()==ecode.getCode();
    }
}
