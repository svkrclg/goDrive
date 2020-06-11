package org.example;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.nio.file.*;

public class UploadFile {
    String accessToken = "";
    public UploadFile getAccessToken(String refreshToken)
    {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(App.serverURL+"/getAccessToken");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("refresh_token", refreshToken));
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
            if(getResponseString.equals("400"))
            {
                System.out.println("Error occured");
                System.exit(0);
            }
            accessToken = getResponseString;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void UploadFileToDrive(File f)
    {
        try {
             URL url = new URL("https://www.googleapis.com/upload/drive/v2/files?uploadType=multipart");
            HttpURLConnection  conn = (HttpURLConnection)url.openConnection() ;
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            String boundary= "2323423423";
            conn.addRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
            conn.addRequestProperty("Authorization", "Bearer "+accessToken);
            JSONObject jsonObject = new JSONObject();
            String mimeType = Files.probeContentType(f.toPath());
            if(mimeType==null || mimeType.equals("null"))
                mimeType = "application/octet-stream";
            jsonObject.put("title", f.getName());
            String body = "\n"+
                          "--"+boundary+"\n"+
                          "Content-Type: application/json; charset=UTF-8"+"\n"+
                          "\n"+
                          jsonObject.toString()+"\n"+
                          "--"+boundary+"\n"+
                          "Content-Type: "+mimeType+"\n"+
                          "\n";
            OutputStream outputStreamToRequestBody =  conn.getOutputStream();
            BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(outputStreamToRequestBody));
            writer.write(body);
            writer.flush();
            FileInputStream inputStreamToLogFile = new FileInputStream(f);
            int bytesRead;
            byte[] dataBuffer = new byte[1024];
            while((bytesRead = inputStreamToLogFile.read(dataBuffer)) != -1) {
                outputStreamToRequestBody.write(dataBuffer, 0, bytesRead);
            }
            writer.write("\n"+
                    "--"+boundary+"--");
            writer.flush();
            writer.close();
            BufferedReader httpResponseReader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String lineRead;
            StringBuilder result = new StringBuilder();
            while((lineRead = httpResponseReader.readLine()) != null) {
                result.append(lineRead);
            }
            JSONParser parser = new JSONParser();
            JSONObject job= (JSONObject) parser.parse(result.toString());
            String id = job.get("id").toString();
            String copiedUrl = job.get("alternateLink").toString();
            System.out.println("File uploaded, URL: "+copiedUrl);
            //copy url to clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(copiedUrl), null);
            //changing permission to all
            ChangePermissionToAll(id);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Problem Occurred");
            e.printStackTrace();
        }
    }
    public void ChangePermissionToAll(String fileId)
    {
        try {
            URL url = new URL ("https://www.googleapis.com/drive/v2/files/"+fileId+"/permissions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer "+accessToken);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type","application/json" );
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("role", "reader");
            jsonObject.put("type", "anyone");
            String s = jsonObject.toString();
            OutputStream os = conn.getOutputStream();
            os.write(s.getBytes());
            BufferedReader httpResponseReader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String lineRead;
            StringBuilder result = new StringBuilder();
            while((lineRead = httpResponseReader.readLine()) != null) {
                result.append(lineRead);
            }
            JSONObject permission = (JSONObject)new JSONParser().parse(result.toString());
            String id = permission.get("id").toString();
            if(id.equals("anyone"))
            {
                System.out.println("Permission changed, anyone with link can view the file");
                System.exit(0);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Problem occurred in changing permission");
            e.printStackTrace();
        }
    }
}
