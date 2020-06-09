package org.example;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App 
{
    private static String client_id = "";
    private static String client_secret  = "";
    private static String authCode =null;
    public static void main( String[] args ) throws IOException, URISyntaxException, InterruptedException, Exception {
        //populate client id and secret from .conf
        ReadPropertyFile();
        if( args.length != 1 )
        {
            print("Invalid argument(s)");
            return;
        }
        if(args[0].equals("auth"))
        {
            //initialize user and key token file
            AuthenticateUser();
            //get refresh token and access token and save refresh token in file
            createRefreshTokenFile();

        }
        else {
            String homePath = System.getProperty("user.home");
            File refreshTokenFile = new File(homePath + "/.refresh_token");
            boolean b = refreshTokenFile.exists();
            if(b== false)
            {
                System.out.println("Refresh token not available, run godrive auth");
                return;
            }
            else
            {
                try {
                    byte[] bytes = new BufferedInputStream(new FileInputStream(homePath + "/.refresh_token")).readAllBytes();
                    String s = new String(bytes);
                    String argument = args[0];
                    if(argument.charAt(0)== '/')
                      argument =  argument.substring(1, argument.length());
                    String filePath = homePath+"/"+argument;
                    print("File path: "+ filePath);
                }catch (IOException e)
                {
                    print("Error in fetching token, run command, 'godrive auth'");
                }
            }
        }
    }
    public static void print(String s)
    {
        System.out.println(s);
    }
    public static void AuthenticateUser() throws URISyntaxException, IOException, InterruptedException {
        //create localserver at port 7341
        Thread t = LocalServer.createServerAndListen();
        //open oauth url in broswer
        String url = "https://accounts.google.com/o/oauth2/v2/auth?scope=https%3A//www.googleapis.com/auth/drive&state=state_parameter&redirect_uri=http%3A//localhost:7341/code&access_type=offline&response_type=code&client_id="+client_id;
        if (Desktop.isDesktopSupported()) {
            // Windows
            Desktop.getDesktop().browse(new URI(url));
        } else {
            // Ubuntu
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("/usr/bin/firefox -new-window " + url);
        }
        System.out.println("Please wait");
         t.join();
         if(LocalServer.authCode==null || LocalServer.authCode.length() == 0)
         {
             System.out.println("Error occurred in obtaining auth code.");
         }
         else
         {
             System.out.println("Got auth code: "+LocalServer.authCode);
             authCode = LocalServer.authCode;
         }
    }
    public static void createRefreshTokenFile() {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://oauth2.googleapis.com/token");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("client_id", client_id));
            nameValuePairs.add(new BasicNameValuePair("client_secret", client_secret));
            nameValuePairs.add(new BasicNameValuePair("code", authCode));
            nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
            nameValuePairs.add(new BasicNameValuePair("redirect_uri", "http://localhost:7341/code"));
            //  System.out.println(nameValuePairs.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()), 2048);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String getResponseString = "";
            getResponseString = sb.toString();
            System.out.println("Auth resp string: " + getResponseString);
            JSONObject jsonObject = new JSONObject();
            JSONParser jsonParser = new JSONParser();
            jsonObject = (JSONObject) jsonParser.parse(getResponseString);
            String access_token = jsonObject.get("access_token").toString();
            String refresh_token = jsonObject.get("refresh_token").toString();
            String homePath = System.getProperty("user.home");
            File f = new File(homePath + "/.refresh_token");
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(homePath + "/.refresh_token");
            fos.write(refresh_token.getBytes());
        }
        catch (IOException e )
        {
            System.out.println("IOException, " );
            e.printStackTrace();
        }
        catch (Exception e)
        {
            System.out.println("Can't get tokens, " );
            e.printStackTrace();
        }
    }
    public static void ReadPropertyFile()
    {
        Properties prop = new Properties();
        File f = new File(App.class.getResource("/config.properties").getFile());
        try {
            FileInputStream fis  = new FileInputStream(f);
            prop.load(fis);
            client_id = prop.getProperty("client_id");
            client_secret = prop.getProperty("client_secret");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

