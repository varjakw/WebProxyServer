import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Hashtable;

public class Handler implements Runnable {
    private Thread transmission;
    Socket socket;
    BufferedReader p2cReader;
    BufferedWriter p2cWriter;
    String requestToHandle;


    public Handler(Socket socket){
        this.socket = socket;
        try{
            this.socket.setSoTimeout(2000);
            p2cReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            p2cWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void useCachedPage(File cachedFile){
        // Read from File containing cached web page
        try{
            // If file is an image write data to client using buffered image.
            String extension = cachedFile.getName().substring(cachedFile.getName().lastIndexOf('.'));

            // Response that will be sent to the server
            String response;
            if((extension.contains(".png")) || extension.contains(".jpg") ||
                    extension.contains(".jpeg") || extension.contains(".gif")){
                // Read in image from storage
                BufferedImage image = ImageIO.read(cachedFile);

                if(image == null ){
                    System.out.println("Image " + cachedFile.getName() + " was null");
                    response = "HTTP/1.0 404 NOT FOUND \n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    p2cWriter.write(response);
                    p2cWriter.flush();
                } else {
                    response = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    p2cWriter.write(response);
                    p2cWriter.flush();
                    ImageIO.write(image, extension.substring(1), socket.getOutputStream());
                }
            }

            // Standard text based file requested
            else {
                BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));

                response = "HTTP/1.0 200 OK\n" +
                        "Proxy-agent: ProxyServer/1.0\n" +
                        "\r\n";
                p2cWriter.write(response);
                p2cWriter.flush();

                String line;
                while((line = cachedFileBufferedReader.readLine()) != null){
                    p2cWriter.write(line);
                }
                p2cWriter.flush();

                // Close resources
                if(cachedFileBufferedReader != null){
                    cachedFileBufferedReader.close();
                }
            }


            // Close Down Resources
            if(p2cWriter != null){
                p2cWriter.close();
            }

        } catch (IOException e) {
            System.out.println("Error Sending Cached file to client");
            e.printStackTrace();
        }
    }

    private void getPage(String urlString){

        try{

            // Compute a logical file name as per schema
            // This allows the files on stored on disk to resemble that of the URL it was taken from
            int extensionIndex = urlString.lastIndexOf(".");
            String extension;

            // Get the type of file
            extension = urlString.substring(extensionIndex, urlString.length());

            // Get the initial file name
            String fileName = urlString.substring(0,extensionIndex);


            // Trim off http://www. as no need for it in file name
            fileName = fileName.substring(fileName.indexOf('.')+1);

            // Remove any illegal characters from file name
            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.','_');

            // Trailing / result in index.html of that directory being fetched
            if(extension.contains("/")){
                extension = extension.replace("/", "__");
                extension = extension.replace('.','_');
                extension += ".html";
            }

            fileName = fileName + extension;



            // Attempt to create File to cache to
            boolean caching = true;
            File fileToCache = null;
            BufferedWriter fileToCacheBW = null;

            try{
                // Create File to cache
                fileToCache = new File("cached/" + fileName);

                if(!fileToCache.exists()){
                    fileToCache.createNewFile();
                }

                // Create Buffered output stream to write to cached copy of file
                fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
            }
            catch (IOException e){
                System.out.println("Couldn't cache: " + fileName);
                caching = false;
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("NPE opening file");
            }





            // Check if file is an image
            if((extension.contains(".png")) || extension.contains(".jpg") ||
                    extension.contains(".jpeg") || extension.contains(".gif")){
                // Create the URL
                URL remoteURL = new URL(urlString);
                BufferedImage image = ImageIO.read(remoteURL);

                if(image != null) {
                    // Cache the image to disk
                    ImageIO.write(image, extension.substring(1), fileToCache);

                    // Send response code to client
                    String line = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    p2cWriter.write(line);
                    p2cWriter.flush();

                    // Send them the image data
                    ImageIO.write(image, extension.substring(1), socket.getOutputStream());

                    // No image received from remote server
                } else {
                    System.out.println("Sending 404 to client as image wasn't received from server"
                            + fileName);
                    String error = "HTTP/1.0 404 NOT FOUND\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    p2cWriter.write(error);
                    p2cWriter.flush();
                    return;
                }
            }

            // File is a text file
            else {

                // Create the URL
                URL remoteURL = new URL(urlString);
                // Create a connection to remote server
                HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
                proxyToServerCon.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                proxyToServerCon.setRequestProperty("Content-Language", "en-US");
                proxyToServerCon.setUseCaches(false);
                proxyToServerCon.setDoOutput(true);

                // Create Buffered Reader from remote Server
                BufferedReader p2sReader = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
                p2cWriter.write("Data recieved from remote server");
                String data;
                while((data = p2sReader.readLine()) != null){ //data from remote server
                    p2cWriter.write(data); //write to client
                    if(caching){  //write to cache
                        fileToCacheBW.write(data);
                    }
                }
                p2cWriter.flush();
                if(p2sReader != null){
                    p2sReader.close();
                }
            }


            if(caching) {
                fileToCacheBW.flush();
                Proxy.addToCache(urlString, fileToCache);
            }
            if(fileToCacheBW != null){
                fileToCacheBW.close();
            }
            if(p2cWriter != null){
                p2cWriter.close();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void takeRequest(String urlString){
        // Extract the URL and port of remote
        String url = urlString.substring(7);
        String array[] = url.split(":");
        url = array[0];
        int port  = Integer.valueOf(array[1]);

        try{
            for(int i=0;i<5;i++){
                // Read the first section to get the type
                p2cReader.readLine();
            }
            InetAddress address = InetAddress.getByName(url); //IP of the url
            Socket proxyToServerSocket = new Socket(address, port); //socket to the relevant server remote
            proxyToServerSocket.setSoTimeout(5000);
            p2cWriter.write("Succesful connection to remote server");
            p2cWriter.flush();
            //o-stream and i-stream to handle data coming to/from remote server and client via proxy
            BufferedWriter p2sWriter = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));
            BufferedReader p2sReader = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



            // Create a new thread to listen to client and transmit to server
            Https https =
                    new Https(socket.getInputStream(), proxyToServerSocket.getOutputStream());

            transmission = new Thread(https);
            transmission.start();


            // Listen to remote server and relay to client
            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToServerSocket.getInputStream().read(buffer);
                    if (read > 0) {
                        socket.getOutputStream().write(buffer, 0, read);
                        if (proxyToServerSocket.getInputStream().available() < 1) {
                            socket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException e) {

            }
            catch (IOException e) {
                e.printStackTrace();
            }


            // Close Down Resources
            if(proxyToServerSocket != null){
                proxyToServerSocket.close();
            }

            if(p2sReader != null){
                p2sReader.close();
            }

            if(p2sWriter != null){
                p2sWriter.close();
            }

            if(p2cWriter != null){
                p2cWriter.close();
            }


        } catch (SocketTimeoutException e) {
            String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
                    "User-Agent: ProxyServer/1.0\n" +
                    "\r\n";
            try{
                p2cWriter.write(line);
                p2cWriter.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        catch (Exception e){
            System.out.println("Error on HTTPS : " + urlString );
            e.printStackTrace();
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

        if(!url.substring(0,4).equals("http")){
            String http = "http://"; //put protocol on front to solve error
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


        // Check request type
        if(method.equals("CONNECT")){
            System.out.println("Connection requested");
            takeRequest(url);
        }

        else{
            // Check if we have a cached copy
            File cachedFile = Proxy.getFromCache(url);
            if(cachedFile != null){
                System.out.println("Page is present in cache");
                useCachedPage(cachedFile);
            } else {
                getPage(url);
            }
        }
    }
}




