package com.pub.jmos;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class JMOS
{
    static boolean debugging = false;
    static boolean listening = true;
    static String settingsFile = "./settings.txt";
    static Map<String, String> settings = null; 
    static JMOSSharedProperties properties = null;
    
    static long numClients = 0;
    
    static
    {
        settings = new HashMap<String, String>();
        properties = new JMOSSharedProperties(10);
    }
    
    public static void main(String []args)
    {
        /**
         * First of all handle all the commandline arguments.
         * Note: this is stupid. Do not sort the array.
         */
        Arrays.sort(args);
        if(Arrays.binarySearch(args, "--debug") >= 0)
            debugging = true;
                
        /**
         * Parse the settings file 
         */
        try
        {
            FileReader fis = new FileReader(settingsFile);
            BufferedReader br = new BufferedReader(fis);
            String input = null;
            while((input = br.readLine()) != null)
            {
                int indexOfComment = input.indexOf('#');
                if(indexOfComment == 0)
                    continue;
                if(indexOfComment > 0)
                    input = input.substring(0, indexOfComment);
                String []parts = input.split("=", 2);
                if(parts.length < 2)
                    throw (new InvalidSettingsFormatException(input));
                settings.put(parts[0].trim(), parts[1].trim());
            }
            
            if(debugging)
            {
                System.out.println("The settings parsed are:");
                
                for(String key : settings.keySet())
                    System.out.println(key + " = " + settings.get(key));
            }
        }
        catch(FileNotFoundException e)
        {
            System.err.println("WARNING: settings file not found. defaults used.");
        }
        catch(IOException e)
        {
            System.err.println("WARNING: Error while reading the settings");
        }
        catch(InvalidSettingsFormatException e)
        {
            System.err.println("WARNING: Invalid format in the settings file");
        }
        catch(Exception e) 
        {
            System.err.println("WARNING: Some other exception\n\t" + e.getMessage());
        }
        
        /**
         * Make changes to behaviour according to settings
         */
        if(settings.containsKey("MaxConnections"))
            properties.setMaxClients(Integer.valueOf(settings.get("MaxConnections")));
        
        /**
         * Start the webserver
         */
        startServer();
    }
    
    public static void startServer()
    {
        ServerSocket server = null;
        try 
        {
            server = new ServerSocket(80);
        }
        catch(IOException e)
        {
            System.err.println("ERROR: Unable to bind the server to port. Stop other programs running on same port.");
            return;
        }
        
        while(listening)
        {
            try
            {
                Socket client = server.accept();
                
                if(debugging)
                    System.out.println("New connection received from: " + client.getInetAddress().toString() + ":" + client.getPort());
                    
                if(!properties.incrementNumClients())
                {
                    client.close();
                    throw (new TooManyClientsException());
                }
                
                ServerThread st = new ServerThread(client);
                st.start();
            }
            catch(IOException e)
            {
                System.err.println("ERROR: Could not handle client request.");
            }
            catch(TooManyClientsException e)
            {
                System.err.println("ERROR: " + e.getMessage() + " Ignoring.");
            }
        }
    }
}