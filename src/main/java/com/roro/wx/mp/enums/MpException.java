package com.roro.wx.mp.enums;

/**
 * 公众号提供服务的过程中产生的异常
 */
public class MpException extends RuntimeException{
    private ErrorCodeEnum errorCode;
    public MpException(ErrorCodeEnum errorCode){
        this.errorCode = errorCode;
    }
    public MpException(){
        this.errorCode = ErrorCodeEnum.UNKNOWN_ERROR;
    }
    public int getErrorCode(){
        return this.errorCode.getCode();
    }
    public String getErrorMsg(){
        return this.errorCode.getDesc();
    }
}
