package com.github.binarywang.demo.wx.mp.utils;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static Date now(){
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }
}
