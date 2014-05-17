package com.pub.jmos;

import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

class ServerThread extends Thread
{
    private Socket clientSocket;
    private String httpCommand;
    private Map<String, String> httpRequestHeaders;
    
    public ServerThread(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
        httpHeaders = new HashMap<String, String> ();
    }
    
    public void run()
    {
        /**
         * Parse out the http headers.
         */
        try
        {
            BufferedReader headerReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String header = null;
            
            httpCommand = headerReader.readLine();
            
            while((header = headerReader.readLine()) != null && !header.equals(""))
            {
                String []parts = header.split(":", 2);
                if(parts.length < 2)
                    continue;
                httpRequestHeaders.put(parts[0], parts[1]);
            }
        }
        catch(IOException e)
        {
            System.out.println("ERROR: An I/O exception occurred. Bailing out!");
        }
        
        if(JMOS.debugging)
        {
            System.out.println("The headers are: ");
            for(String key : httpRequestHeaders.keySet())
                System.out.println(key + " = " + httpRequestHeaders.get(key));
        }
        
        /**
         * Verify the authenticity of headers.
         */
        
        /**
         * Pump out the response headers, and response.
         */
        
        /**
         * Clean up
         */
        JMOS.properties.decrementNumClients();
        try
        {
            clientSocket.close();
        }
        catch(IOException e)
        {
            System.err.println("ERROR: I/O exception while trying to close the connection. Well no harm done! ;-)");
        }
    }
}