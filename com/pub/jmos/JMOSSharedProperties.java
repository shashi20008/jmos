package com.pub.jmos;

public class JMOSSharedProperties
{
    private long numClients;
    private long maxClients;
    
    public JMOSSharedProperties(int maxClients)
    {
        this.maxClients = maxClients;
    }
    
    public boolean incrementNumClients()
    {
        if(numClients > maxClients)
            return false;
            
        synchronized(this)
        {
            numClients++;
        }
        return true;
    }
    
    public boolean decrementNumClients()
    {
        if(numClients <= 0)
            return false;
            
        synchronized(this)
        {
            numClients--;
        }
        return true;
    }
    
    public void setMaxClients(int max)
    {
        maxClients = max;
    }
}