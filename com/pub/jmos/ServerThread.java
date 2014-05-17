package com.pub.jmos;

import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.HashMap;

class ServerThread extends Thread
{
    private Socket clientSocket;
    private String httpCommand;
    private String responseStatus = "";
    private Map<String, String> httpRequestHeaders;
    private Map<String, String> httpResponseHeaders;
    private Map<String, String> getQueryParams;
    private Map<String, String> postQueryParams;
    
    public ServerThread(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
        httpRequestHeaders = new HashMap<String, String> ();
        httpResponseHeaders = new HashMap<String, String> ();
        getQueryParams = new HashMap<String, String> ();
        postQueryParams = new HashMap<String, String> ();
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
         * Verify the authenticity of headers. And create response Headers.
         */
        String []commandParts = httpCommand.split(" ", 3);
        File file = null;
        if(commandParts[0].equalsIgnoreCase("POST") || commandParts[0].equalsIgnoreCase("PUT"))
        {
            responseStatus = "HTTP/1.1 405 Method Not Allowed";
            httpResponseHeaders.put("Allow", "GET, HEAD");
        }
        else
        {
            String requestedURI = commandParts[1];
            String [] URIParts = requestedURI.split("[?]", 2);
            String filePath = URIParts[0];
            String queryParams = "";
            if(URIParts.length > 1)
                queryParams = URIParts[1];
            if(JMOS.debugging)
            {
                System.out.println("file path = " + filePath);
                System.out.println("query params = " + queryParams);
            }
            
            file = new File(JMOS.settings.get("DocRoot").replace("/", "\\") + filePath.replace("/", "\\"));
            if(file.isDirectory())
            {
                int counter = 1;
                String dirname = file.getAbsolutePath();
                file = new File(dirname + "\\index." + JMOS.defExtension[0]);
                while(counter < JMOS.defExtension.length && !file.exists())
                    file = new File(dirname + "\\index." + JMOS.defExtension[counter++]);
                if(!file.exists())
                {
                    responseStatus = "HTTP/1.1 404 Not Found";
                    if(JMOS.settings.containsKey("404MessageFile"))
                    {
                        file = new File(JMOS.settings.get("DocRoot").replace("/", "\\") + "\\" + JMOS.settings.get("404MessageFile"));
                        if(file.exists())
                        {
                            httpResponseHeaders.put("Content-Type", "text/html");
                            httpResponseHeaders.put("Content-Length", "" + file.length());
                        }
                    }
                }
                else
                {
                    responseStatus = "HTTP/1.1 200 OK";
                    // Determine the type of file. (based on extension for now, may be some Magic later.)
                    httpResponseHeaders.put("Content-Type", JMOSHelper.getMIME(file.getName()));
                    httpResponseHeaders.put("Content-Length", "" + file.length());
                }
            }
            else if(file.exists())
            {
                responseStatus = "HTTP/1.1 200 OK";
                httpResponseHeaders.put("Content-Type", JMOSHelper.getMIME(file.getName()));
                httpResponseHeaders.put("Content-Length", "" + file.length());
            }
            else
            {
                responseStatus = "HTTP/1.1 404 Not Found";
                if(JMOS.settings.containsKey("404MessageFile"))
                {
                    file = new File(JMOS.settings.get("DocRoot").replace("/", "\\") + "\\" + JMOS.settings.get("404MessageFile").replace("/", "\\"));
                    if(file.exists())
                    {
                        httpResponseHeaders.put("Content-Type", "text/html");
                        httpResponseHeaders.put("Content-Length", "" + file.length());
                    }
                }
            }
        }
        
        /**
         * Pump out the response headers, and response.
         */
        try
        {
            OutputStream out = clientSocket.getOutputStream();
            out.write(new String(responseStatus + "\r\n").getBytes());
            
            // A hack for giving user friendly 404 message even if setting doesn't point to a valid 404 file.
            String error404Message = null;
            if(file != null && !file.exists())
            {
                error404Message = "<html><head><title>404 Not Found</title></head><body><h1>Could not locate the item you were looking for</h1></body></html>";
                httpResponseHeaders.put("Content-Type", "text/html");
                httpResponseHeaders.put("Content-Length", "" + error404Message.length());
            }
            
            for(String key : httpResponseHeaders.keySet())
            {
                out.write(new String(key + ":" + httpResponseHeaders.get(key) + "\r\n").getBytes());
                if(JMOS.debugging)
                    System.out.println(key + ":" + httpResponseHeaders.get(key));
            }
            
            out.write("\r\n".getBytes());
            
            if(file != null)
            {
                if(file.exists())
                {
                    FileInputStream fis = new FileInputStream(file);
                    byte []canister = new byte[512];
                    int numBytes = 0;
                    while((numBytes = fis.read(canister)) >= 0)
                    {
                        out.write(canister, 0, numBytes);
                    }
                    fis.close();
                }
                else
                {
                    out.write(error404Message.getBytes());
                }
            }
        }
        catch(IOException e)
        {
            System.err.println("ERROR: I/O exception while sending data to client. Connection will be abandoned.\n" + e.getMessage() );
        }
        
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