package com.boxysystems.scriptmonkey.intellij.util;

import java.util.Date;

/**
 * Created by : Dmitry Karlinsky
 * Date: 6/12/2014 8:09 PM
 */
public class LoggingUtil
{
    public static String withDate(String text)
    {
        return "["+new Date()+"] "+text;
    }
}
