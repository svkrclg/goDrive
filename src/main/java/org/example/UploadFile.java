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
            Thread t= new Thread(new Runnable() {
                @Override
                public void run() {
                    String anim= "|/-\\";
                    int x=0;
                    while(true){
                        String data = "\r" + anim.charAt(x % anim.length()) + " Fetching access token.";
                        try {
                            System.out.write(data.getBytes());
                            Thread.sleep(100);
                        } catch (IOException e) {
                            System.out.print("\033[2K"+"\rProblem Occurred.\n");
                        } catch (InterruptedException e) {
                            return;
                        }
                        x++;
                    }
                }
            });
            t.start();
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
            //stop thread
            t.interrupt();
            if(getResponseString.equals("400"))
            {
                System.out.write(new String("\rError occurred").getBytes());
                System.exit(0);
            }
            System.out.write(new String("\rAccess token fetching successful.\n").getBytes());
            accessToken = getResponseString;
        }
        catch (UnsupportedEncodingException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (ClientProtocolException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (IOException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        }
        return this;
    }

    public void UploadFileToDrive(File f)
    {
        try {
            System.out.print(new String("Sending metadata..."));
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
            System.out.print("\033[2K"+"\rMetadata sent. \n");
            FileInputStream inputStreamToFile = new FileInputStream(f);
            long l = f.length();
            int bytesRead;
            long totalBytesRead=0;
            byte[] dataBuffer = new byte[1024];
            while((bytesRead = inputStreamToFile.read(dataBuffer)) != -1) {
                totalBytesRead+=bytesRead;
                outputStreamToRequestBody.write(dataBuffer, 0, bytesRead);
                int percentage = (int)(totalBytesRead/l) * 100;
                System.out.print("\033[2K"+"\r"+percentage+" Uploading...");
            }
            writer.write("\n"+
                    "--"+boundary+"--");
            writer.flush();
            writer.close();
            System.out.print("\033[2K"+"\rUploaded.\nWaiting for response...");
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
            System.out.print("\rFile uploaded, URL: "+copiedUrl+"\n");
            //copy url to clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(copiedUrl), null);
            //changing permission to all
            System.out.print("\rChanging permission...");
            ChangePermissionToAll(id);

        } catch (UnsupportedEncodingException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (ClientProtocolException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (IOException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (ParseException e) {
            System.out.println("\033[2K"+"\rProblem Occurred");
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
                System.out.print("\033[2K"+"\rPermission changed, anyone with link can view the file.\n");
                System.exit(0);
            }
        } catch (MalformedURLException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (IOException e) {
            System.out.print("\033[2K"+"\rProblem Occurred.\n");
        } catch (ParseException e) {
            System.out.println("\033[2K"+"\rProblem occurred in changing permission.\n");
        }
    }
}
