# goDrive
A command line tool to upload files to your Google drive account.

## What it does?
It uses the Google [OAuth 2.0](developers.google.com/identity/protocols/oauth2) to obtain the end user's Access Token and send a POST request to [Google drive API](https://developers.google.com/drive). By default it also changes the permission of the uploaded to make it accessible to everyone with the link.

## The Workflow

Let us first know some of the important terms involved in OAuth 2.0.
- **Client Id** : This is public identifier of each application registered for enabling OAuth 2.0 functionality in Google API console.
- **Client Secret** : This is kept secret from user's because it used to obtain the access token along with client_id. (hence this request is send from our Server and not from user application).
- **Authorization code** : This code is obtained after successful authentication of user in OAuth 2.0, which is performed by user in browser using a consent screen. This code is send back as GET parameter to our redirect URI which is basically a local server in user application on port ```7341```
- **Refresh Token** : This token is play a important role in avoiding the user's consent to access there Google drive. It is saved in user's machine and is send to our server to fetch access token (if access token is expired). The Refresh token is send by Google Oauth server only if the GET parameter ```access_type```  is set to ```offline``` during user authentication.
- **Access Token** : This is used every time we hit the Google APIs,

### Tokens and there expiry time:
- **Authorization Code** : It is very short-lived, about 10 minutes, and can be used only once to obtain access- token and refresh token.
- **Access Token** : It can be alive for 1 hours after the generation by Google Oauth endpoints.
- **Refresh_token** : It cannot be expired (can expired if not used for continous six months) and hence is stored in user machine If your application loses the refresh token, the user will need to repeat the OAuth 2.0 consent flow to obtain a new refresh token.

The procedure of creating request for obtaining token is available in [Google OAuth 2.0 protocols](https://developers.google.com/identity/protocols/oauth2/web-server).
Now let's talk about workflow,

**The CLI currently offer's have two arguments:**
### 1) ```godrive --auth```
Used to authenticate the user through Oauth consent flow.
- The URL for this request is :

```https://accounts.google.com/o/oauth2/v2/auth?scope=<OUR APPLICATION SCOPE>&access_type=offline&include_granted_scopes=true&response_type=code&state=state_parameter&redirect_uri=<REDIRECT_URI>&client_id=<OUR APPLICATION CLIENT_ID>```

To have read and write permission to Google drive the **Scope** is ```https://www.googleapis.com/auth/drive```

After successful user grant, the result in available in redirect URI as GET parameter
```http://localhost:7341/code/code=<AUTHORIZATION CODE>```

- This authorization code is used obtain access and Refresh token by hitting the URL with POST for this is:
```
https://oauth2.googleapis.com/token
Content-Type: application/x-www-form-urlencoded

code=<AUTH_CODE>&
client_id=<CLIENT_ID>&
client_secret=<CLIENT_SECRET>&
redirect_uri=https://localhost:7341/code&
grant_type=authorization_code
```
The response is in JSON:

```{
  "access_token": "1/fFAGRNJru1FTz70BzhT3Zg",
  "expires_in": 3920,
  "token_type": "Bearer",
  "scope": "https://www.googleapis.com/auth/drive",
  "refresh_token": "1//xEoDL4iW3cxlI7yDbSRFYNG01kVKM2C-259HOF2aQbI"
}
```
Since this request require client_secret, it is not intiated by user directly, but using our Server, after user hits ```<serverurl>/getRefreshToken``` from application with ```auth_code``` as paramater value of POST request.

<img src="https://github.com/svkrclg/goDrive/blob/master/auth.png" height= "500" width="500">

In the end it creates file ```.refresh_token``` with refresh_token String, in user's home directory.
### 1) ```godrive --upload <filepath>```
Used to upload a new file to Google drive using,
The API request need Access token in the POST request, hence send the request to get access token:
```
https://oauth2.googleapis.com/token
Content-Type: application/x-www-form-urlencoded

refresh_token=<REFRESH_TOKEN>&
client_id=<CLIENT_ID>&
client_secret=<CLIENT_SECRET>&
grant_type=refresh_token
```
Response recieved from OAuth:
```
{
"access_token": "ya29.a0AfH6SMBquJL-zEqZAGba38s3zFTVSxk-m7CbZJdsBcUxbzlOQ5eTY65CIPZLcUUtVQ62P0MkIdVD6mvsPLFOOc9Jm5FfpATL_6uj9SMd_8-5278GFWlhDL0ppmgknjwnepOZm1LC1DmDEHy7KOJK9bDLWTEzaCXteedm",
"expires_in": 3599,
"scope": "https://www.googleapis.com/auth/drive",
"token_type": "Bearer"
}
 ```
 This request requires client_secret, hence initiated from goDrive server, after user send refresh token to goDrive server for that operation, which sends back access token to user.
 From access token we can now finally start uploading files,
 The POST request is send with multipart request body,
 ```
 POST /upload/drive/v2/files?uploadType=multipart HTTP/1.1
 Host: googleapi.com
 Authorization: Bearer <ACCESS TOKEN>
 Content-Type: multipart/related; boundary=<boundary_string>
 
 --<boundary_string>
 Content-Type: application/json; charset=UTF-8
 
 {
 title: "<file_title>",
 description: "<description>"
 }
 --<boundary_string>
 Content-Type: <MIME-type>
 
 <file binary data>
 --<boundary_string>--
 ```
 The response contain JSON data, with download URL and id of file.
 
 <img src="https://github.com/svkrclg/goDrive/blob/master/upload.png" height= "500" width="500">
 
 ## Building From source:
 Requirements:
 - Linux
 - JDK
 - Maven
 
 Building:
 - Clone the repository:
 ```https://github.com/svkrclg/goDrive.git```
 - Change the directory to the repository directory.
 - run command ```mvn package```
 - Move the ```goDrive.jar``` file (inside directory /target) to ```home``` directory.
 - Move the ```.script.sh``` file (present in root folder)to ```home``` directory.
 - Append the code ```source .script.sh``` at the end of ```.bashrc``` present in home directory.
 - Open terminal, and **ENJOY**:+1: 
 
 <img src="https://github.com/svkrclg/goDrive/blob/master/output.png">
 
 ##Usage
- For authentication
 > godrive -a
- For uploading
 > godrive -u \<filepath\>


<img src="https://github.com/svkrclg/goDrive/blob/master/help.png">
 

 *\*goDrive server is hosted in Heroku platform, which contain the client_id & client_secret.* 
 
