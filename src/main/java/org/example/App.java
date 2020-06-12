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
import org.json.simple.parser.ParseException;
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
    public static String serverURL= "https://godriveserver.herokuapp.com";
    private static String authCode =null;
    public static void main( String[] args ) throws IOException, URISyntaxException, InterruptedException, Exception {
        //populate client id and secret from .conf
        ReadPropertyFile();
        if( args.length == 0 )
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
            String filePath = args[0];
            if(filePath.charAt(0) =='/')
                filePath= filePath.substring(1, filePath.length());
            if(filePath.length()!=0 && filePath.charAt(filePath.length()-1)=='/')
                filePath= filePath.substring(0, filePath.length()-1);
            String actualFilePath = System.getProperty("user.dir")+File.separator+filePath;
            System.out.println("FilePath: "+actualFilePath);
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
                    File f = new File(actualFilePath);
                    if(f.exists() ==false || f.isFile() == false)
                    {
                        print("Cannot find file.");
                        System.exit(0);
                    }
                    //upload file
                    new UploadFile().getAccessToken(s).UploadFileToDrive(f);
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
             authCode = LocalServer.authCode;
             System.out.print("Auth code received.\n Fetching Refresh token...");
         }

    }
    public static void createRefreshTokenFile() {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(serverURL+"/getRefreshToken");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("code", authCode));
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
            if(sb.toString().equals("400"))
            {
                System.out.print("\033[2K"+"Error occurred in fetching refresh token\n");
                return;
            }
            String refresh_token = sb.toString();
            String homePath = System.getProperty("user.home");
            File f = new File(homePath + "/.refresh_token");
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(homePath + "/.refresh_token");
            fos.write(refresh_token.getBytes());

            System.out.print("\033[2K"+"\rRefresh Token fetching successful.\n Authentication successful.\n");
        }
        catch (IOException e )
        {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        }
    }
    public static void ReadPropertyFile()
    {
        Properties prop = new Properties();
        InputStream f = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
        try {
            prop.load(f);
            client_id = prop.getProperty("client_id");
        } catch (FileNotFoundException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (IOException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        }

    }
}

