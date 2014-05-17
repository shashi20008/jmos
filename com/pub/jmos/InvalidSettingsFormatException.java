package com.pub.jmos;

public class InvalidSettingsFormatException extends Exception
{
    public InvalidSettingsFormatException()
    {
        super("The settings file has invalid format.");
    }
    
    public InvalidSettingsFormatException(String err)
    {
        super("The settings file has invalid format.\n\t" + err);
    }
}