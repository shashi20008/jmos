package com.pub.jmos;

public class TooManyClientsException extends Exception
{
    public TooManyClientsException()
    {
        super("Too many simultaneous clients.");
    }
}