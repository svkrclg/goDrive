package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class LocalServer {
    static String authCode;
    public static Thread createServerAndListen()
    {
        Thread t = null;
        try {
            final ServerSocket ss = new ServerSocket(7341);
            t= new Thread() {
                @Override
                public void run() {
                    try {

                        Socket client = ss.accept();
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(client.getInputStream()));
                        String data = "";
                        while ((data = in.readLine()) != null) {
                          //  System.out.println("\r\nMessage from " + ": " + data);
                            if(data.contains("state_parameter"))
                            {
                                String s[] = data.split("=");
                                //System.out.println(Arrays.deepToString(s));
                                String temp = s[2];
                                authCode = temp.split("&")[0];
                                //System.out.println("AuthCode: "+authCode);
                                break;
                            }
                            if (data.isEmpty()) {
                                break;
                            }
                        }

                       // System.out.println("Done receiving");

                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                        out.write("HTTP/1.0 200 OK\r\n");
                        out.write("Server: Java/0.8.4\r\n");
                        out.write("Content-Type: text/html\r\n");
                        out.write("\r\n");
                        out.write("<P>You can close browser</P>");
                        out.close();
                        in.close();
                        client.close();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            };

        } catch (IOException e) {
            e.printStackTrace();
        }
        t.start();
        return t;
    }
}
