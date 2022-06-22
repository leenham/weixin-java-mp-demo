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
    DENY_BLACKLIST_USER(900008,"您已被列入黑名单,无法使用功能,请联系开发者解除."),
    UNKNOWN_ERROR(900099,"系统未知错误"),

    /*处理暗号图报错*/
    CIPHER_ILLEGAL_ANSWER(900101,"暗号图答案格式异常"),
    CIPHER_ILLEGAL_PIC(900102,"读取暗号图失败,请发送官方暗号图原图"),
    CIPHER_WITHOUT_QRCODE(900103,"图片指定区域内未能检测出二维码,请保存原图后再次发送,不要使用手机截图"),
    CIPHER_WRONG_QRCODE(900104,"检测出不属于暗号图的二维码"),
    NO_COMMIT_CIPHER(900105,"请先上传暗号图,再输入答案"),
    RECENT_CIPHER_ERROR(900106,"最近一次提交的暗号图异常,请上传官方原图后再输入答案"),
    CIPHER_RETRIVAL_ERROR(900107,"检索暗号图时发生异常"),
    FAIL_UPDATE_CIPHER_POOL(900108,"更新暗号池时发生异常"),
    UNABLE_RESOLVE_CIPHER(900109,"无法处理该暗号图"),
    CIPHER_SMALL_SIZE(900110,"图片尺寸太小,请保存暗号图原图(而非截图)后再发送"),
    FAIL_ADD_NEW_CIPHER(900111,"保存新暗号图时发生异常,请联系开发人员"),
    FAIL_DEL_CIPHER(900112,"试图删除暗号图时出错,请联系开发人员"),
    CIPHER_UNKNOWN_ERROR(900199, "处理暗号图时未知异常"),

    /*处理答题报错*/
    NO_QUIZ_FOUND(900201,"答题助手还在学习中,换个检索词试试吧"),
    QUIZ_ILLEGAL_UPDATE(900202,"更新题库时发生异常,请检查输入格式"),
    QUIZ_WRONG_LABEL(900203,"题目编号格式错误,请检查输入格式"),
    QUIZ_UNEXIST_LABEL(900204,"臣妾找不到这一题啊"),
    NO_RECENT_COMMIT_QUIZ(900205,"先得检索到单条记录，才能修改哈!"),
    QUIZ_WRONG_COMMAND(900206,"修改题目的指令格式异常"),
    QUIZ_WRONG_CHOICE(900207,"修改选项的指令格式异常"),
    QUIZ_DELETE_ERROR(900208, "删除题库中题目时出错"),
    KEYWORD_TOO_LONG(900209,"关键词也太长了,要不，换个短点儿的？"),
    QUIZ_WRONG_INDEX(900210,"这个题目编号,我看不懂"),
    QUIZ_UNKNOWN_ERROR(900299,"答题助手...发生...未知...异常..@#$%$&^!"),

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
