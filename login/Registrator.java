package login;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Registrator {
    private URL serverURL;
    private HttpURLConnection serverConnection;
    private OutputStream serverStream;

    public Registrator(String url) throws IOException {
        this.serverURL = new URL(url);
        this.serverConnection = (HttpURLConnection) this.serverURL.openConnection();
        this.serverConnection.setRequestMethod("POST");
        this.serverConnection.setDoOutput(true); //Because we are outputting values to the server
        this.serverStream = this.serverConnection.getOutputStream();
    }

    public boolean register(String username, String password) throws IOException {
        String postArguments = getPostString(username, password);
        this.serverStream.write(postArguments.getBytes());
        this.serverStream.flush();
        return this.serverConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    public String getPostString(String username, String password) {
        StringBuilder sb = new StringBuilder();
        sb.append("pv_login=").append(username);
        sb.append("&pv_password=").append(password);
        return sb.toString();
    }

    public String getServerMessage() throws IOException {
        return this.serverConnection.getResponseMessage();
    }
}
