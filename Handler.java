import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Hashtable;

public class Handler implements Runnable{
    Socket socket;
    private Thread c2s;
    BufferedReader p2cReader;
    BufferedWriter p2cWriter;
    String requestToHandle;
    File cachedFile;

    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(3000);
        p2cReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        p2cWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

    }

    private void useCachedPage(File file) throws FileNotFoundException {

        BufferedReader cacheReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String message = "Request succesful";
        try {
            p2cWriter.write(message);
            p2cWriter.flush();
            String string = cacheReader.readLine();
            while (string != null) {
                p2cWriter.write(string);
            }
            p2cWriter.flush();
            cacheReader.close();
            p2cWriter.close();
        }catch (IOException e) {
            System.out.println("Error with accessing cached page");
        }
    }

    private void getPage(String url) {

        int dotIndex = url.lastIndexOf(".");
        String extension = url.substring(dotIndex, url.length());
        String name = url.substring(0,dotIndex);

        //remove slashes and dots from file names
        name = name.replace("/","_");
        name = name.replace(".","-");
        //add extension
        name = name + extension;

        File cacheFile = new File("cache/" + name);
        if(!cacheFile.exists()){
            try {
                cacheFile.createNewFile();
            } catch (IOException e) {
                    System.out.println("Error with creating cache page");
                e.printStackTrace();

            }
        }
        BufferedReader remoteReader = null;
        BufferedWriter cacheWriter = null;
        try {
            //writing to file in cache
            cacheWriter = new BufferedWriter(new FileWriter(cacheFile));

            URL link = new URL(url);
            //connect to remote server
            HttpsURLConnection conn = (HttpsURLConnection) link.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Language", "en-UK");
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            //reader of remote server
            //proxyToServerBR
            remoteReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            p2cWriter.write("Request successful");

            String message = remoteReader.readLine();
            while (message != null) {
                //send to client
                p2cWriter.write(message);
                //write to cache
                cacheWriter.write(message);
            }
            p2cWriter.flush();
            remoteReader.close();
            cacheWriter.flush();
            Proxy.addToCache(url, cacheFile);
            cacheWriter.close();
            p2cWriter.close();

        } catch(IOException e){
            System.out.println("Error sending to client");
            e.printStackTrace();
        }
      //here
    }

    private void takeRequest(String url) throws IOException {
        String link = url.substring(7);
        String array[] = link.split(":");
        link = array[0];
        int port = Integer.parseInt(array[1]);

        for(int i=0;i<5;i++){
            p2cReader.readLine();
        }
        //ip of url
        InetAddress ip = InetAddress.getByName(url);
        //open socket to the remote server
        Socket p2sSocket = new Socket(ip,port);
        p2sSocket.setSoTimeout(10000);
        //send this connection to the client
        p2cWriter.write("Connection made");
        p2cWriter.flush();

        //thread to handle simultaneous data
        BufferedWriter p2sWriter = new BufferedWriter(new OutputStreamWriter(p2sSocket.getOutputStream()));
        BufferedReader p2sReader = new BufferedReader(new InputStreamReader(p2sSocket.getInputStream()));

        c2sHttps c2sHttps = new c2sHttps(socket.getInputStream(),p2sSocket.getOutputStream());
        c2s = new Thread(c2sHttps);
        c2s.start();

        //listen to remote and push to client
        try {
            byte[] buffer = new byte[4096];
            int in;
            do {
                in = p2sSocket.getInputStream().read(buffer);
                if (in > 0) {
                    socket.getOutputStream().write(buffer, 0, in);
                    if (p2sSocket.getInputStream().available() < 1) {
                        socket.getOutputStream().flush();
                    }
                }
            } while (in >= 0);
        }
        catch (SocketTimeoutException e) {
            System.out.println("SocketTimeoutException");
        }
        catch (IOException e) {
            System.out.println("Issue with remote listen & push to client");
        }
        if(p2sSocket != null){
            p2sSocket.close();
        }
        if(p2sReader != null){
            p2sWriter.close();
        }
        if(p2sWriter != null){
            p2sWriter.close();
        }
        if(p2cWriter != null){
            p2cWriter.close();
        }
    }

    @Override
    public void run() {
        try {
            requestToHandle = p2cReader.readLine();
        } catch (IOException e) {
            System.out.println("Error reading the HTTPS requestToHandle");
        }

        System.out.println("Request received: " + requestToHandle);

        //get url from the string
        String method = requestToHandle.substring(0,requestToHandle.indexOf(' '));
        String url = requestToHandle.substring(requestToHandle.indexOf(' ')+1);
        url = url.substring(0,url.indexOf(' '));

        //append protocol to front
        if(!url.substring(0,5).equals("https")){
            String http = "https://";
            url = http + url;
        }

        Hashtable temp = Proxy.blocked;
        //check if the page is blocked
        if(temp.containsKey(url)){
            System.out.println( url + " is blocked.\n Contact the administrator.");
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                String error = "Site is blocked!";
                bw.write(error);
                bw.flush();
            } catch (IOException e) {
                System.out.println("Error handling a blocked site");
            }
        }

        /////////////////////

        if(method == "CONNECT"){
            try {
                System.out.println("Connection requested");
                takeRequest(url);
            } catch (IOException e) {
                System.out.println("Error with takeRequest");
            }
        } else {
            //check cache
            File cachedFile = Proxy.getFromCache(url);
            if(cachedFile != null){
                System.out.println("Site found in cache");
                try {
                    useCachedPage(cachedFile);
                } catch (IOException e) {
                   System.out.println("Error using cached file");
                }
            } else{
                getPage(url);
            }
        }

    }
}
