import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

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
    ////////////////////////////

    private void useCachedPage(File file) throws IOException {
        BufferedReader cacheReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String message = "Proxy OK";
        writer.write(message);
        writer.flush();

        String string = cacheReader.readLine();
        while (string != null){
            writer.write(string);
        }
        writer.flush();

        cacheReader.close();
        writer.close();
    }

    private void getPage(String url) throws IOException {
        BufferedReader remoteReader = null;
        int index = url.lastIndexOf(".");
        String extension = url.substring(index, url.length());
        String name = url.substring(0,index);

        File cacheFile = new File("cache/" + name);
        //buff stream to write to cached file
        BufferedWriter cw = new BufferedWriter(new FileWriter(cacheFile));

        //if the file is a picture
        if(extension.contains(".png") || extension.contains(".jpg")){
            URL link = new URL(url);
            BufferedImage pic = ImageIO.read(link);
            //cache the picture
            ImageIO.write(pic, extension.substring(1),cacheFile);
            String message = "Proxy OK";
            writer.write(message);
            writer.flush();

            //send image to client
            ImageIO.write(pic, extension.substring(1),socket.getOutputStream());
        }else{
            URL link = new URL(url);
            //connect to remote server
            HttpsURLConnection proxy2Server = (HttpsURLConnection)link.openConnection();
            proxy2Server.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            proxy2Server.setRequestProperty("Content-Language", "en-UK");
            proxy2Server.setUseCaches(false);
            proxy2Server.setDoOutput(true);

            //reader of remote server
            //proxyToServerBR
            remoteReader = new BufferedReader(new InputStreamReader(proxy2Server.getInputStream()));
            writer.write("200");
            String message = remoteReader.readLine();
            while(message != null){
                //send to client
                writer.write(message);
                //write to cache
                cw.write(message);
            }

        }
        writer.flush();
        remoteReader.close();

        cw.flush();
        Proxy.addToCache(url, cacheFile);
        cw.close();
        writer.close();
    }

    private void takeRequest(String url) throws IOException {
        String link = url.substring(7);
        String array[] = link.split(":");
        link = array[0];
        int port = Integer.valueOf(array[1]);

        for(int i=0;i<5;i++){
            reader.readLine();
        }
        //ip of url
        InetAddress ip = InetAddress.getByName(url);
        //open socket to the remote server
        Socket p2sSocket = new Socket(ip,port);
        p2sSocket.setSoTimeout(10000);
        //send this connection to the client
        writer.write("200 Connection made");
        writer.flush();

        BufferedWriter p2sWriter = new BufferedWriter(new OutputStreamWriter(p2sSocket.getOutputStream()));
        BufferedReader p2sReader = new BufferedReader(new InputStreamReader(p2sSocket.getInputStream()));

        c2sHttps c2sHttps = new c2sHttps(socket.getInputStream(),p2sSocket.getOutputStream());
        Thread c2s = new Thread(c2sHttps);
        c2s.start();

        //listen to remote and push to client
        try {
            byte[] buffer = new byte[4096];
            int read;
            do {
                read = p2sSocket.getInputStream().read(buffer);
                if (read > 0) {
                    socket.getOutputStream().write(buffer, 0, read);
                    if (p2sSocket.getInputStream().available() < 1) {
                        socket.getOutputStream().flush();
                    }
                }
            } while (read >= 0);
        }
        catch (SocketTimeoutException e) {
            writer.write("Timeout!");
            writer.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        if(p2sSocket != null){
            p2sSocket.close();
        }

        if(p2sWriter != null){
            p2sWriter.close();
        }
        if(writer != null){
            writer.close();
        }
    }

    @Override
    public void run() {
        try {
            requestToHandle = p2cReader.readLine();
        } catch (IOException e) {
            System.out.println("IOException");
        }

        System.out.println("Request received: " + requestToHandle);

        //get url from the string
        String method = requestToHandle.substring(0,requestToHandle.indexOf(' '));
        String url = requestToHandle.substring(requestToHandle.indexOf(' ')+1);
        url = url.substring(0,url.indexOf(' '));

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
                System.out.println("IOException");
            }
        }

        /////////////////////

        if(method == "CONNECT"){
            try {
                System.out.println("Connection requested");
                takeRequest(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //check cache
            File cachedFile = Proxy.getFromCache(url);
            if(cachedFile != null){
                System.out.println("Site found in cache");
                useCachedPage(cachedFile);
            } else{
                getPage(url);
            }
        }

    }

    private void requestBlocked() throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bw.write("site blocked. contact admin");
        bw.flush();
    }
}
