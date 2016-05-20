package com.example.android.bluetoothlegatt;

import java.net.*;
import java.io.*;


public class musicLoverConnection
{
    public static String setVolume(int volume)
    {
        return getUrlContents("http://10.1.176.142:3000/api/setVolume?volume="+volume);
    }

    public static String getVolume()
    {
        return getUrlContents("http://10.1.176.142:3000/api/getVolume");
    }


    public static String getUrlContents(String theUrl)
    {
        StringBuilder content = new StringBuilder();

        // many of these calls can throw exceptions, so i've just
        // wrapped them all in one try/catch statement.
        try
        {
            // create a url object
            URL url = new URL(theUrl);

            // create a urlconnection object
            URLConnection urlConnection = url.openConnection();

            // wrap the urlconnection in a bufferedreader
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String line;

            // read from the urlconnection via the bufferedreader
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line + "\n");
            }
            bufferedReader.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return content.toString();
    }
}