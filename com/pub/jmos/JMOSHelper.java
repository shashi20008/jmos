package com.pub.jmos;

import java.net.URLConnection;

public class JMOSHelper
{
    public static String getMIME(String filename)
    {
        String retVal = URLConnection.guessContentTypeFromName(filename);
        if(retVal == null || retVal.equals(""))
            retVal = "text/plain";
        System.out.println(retVal);
        return retVal;
    }
}