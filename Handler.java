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


    /**
     * Sends the specified cached file to the client
     * @param cachedFile The file to be sent (can be image/text)
     */
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


    /**
     * Sends the contents of the file specified by the urlString to the client
     * @param urlString URL ofthe file requested
     */
    private void sendNonCachedToClient(String urlString){

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
                BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));


                // Send success code to client
                String line = "HTTP/1.0 200 OK\n" +
                        "Proxy-agent: ProxyServer/1.0\n" +
                        "\r\n";
                p2cWriter.write(line);


                // Read from input stream between proxy and remote server
                while((line = proxyToServerBR.readLine()) != null){
                    // Send on data to client
                    p2cWriter.write(line);

                    // Write to our cached copy of the file
                    if(caching){
                        fileToCacheBW.write(line);
                    }
                }

                // Ensure all data is sent by this point
                p2cWriter.flush();

                // Close Down Resources
                if(proxyToServerBR != null){
                    proxyToServerBR.close();
                }
            }


            if(caching){
                // Ensure data written and add to our cached hash maps
                fileToCacheBW.flush();
                Proxy.addToCache(urlString, fileToCache);
            }

            // Close down resources
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


    /**
     * Handles HTTPS requests between client and remote server
     * @param urlString desired file to be transmitted over https
     */
    private void takeRequest(String urlString){
        // Extract the URL and port of remote
        String url = urlString.substring(7);
        String pieces[] = url.split(":");
        url = pieces[0];
        int port  = Integer.valueOf(pieces[1]);

        try{
            // Only first line of HTTPS request has been read at this point (CONNECT *)
            // Read (and throw away) the rest of the initial data on the stream
            for(int i=0;i<5;i++){
                p2cReader.readLine();
            }

            // Get actual IP associated with this URL through DNS
            InetAddress address = InetAddress.getByName(url);

            // Open a socket to the remote server
            Socket proxyToServerSocket = new Socket(address, port);
            proxyToServerSocket.setSoTimeout(5000);

            // Send Connection established to the client
            String line = "HTTP/1.0 200 Connection established\r\n" +
                    "Proxy-Agent: ProxyServer/1.0\r\n" +
                    "\r\n";
            p2cWriter.write(line);
            p2cWriter.flush();



            // Client and Remote will both start sending data to proxy at this point
            // Proxy needs to asynchronously read data from each party and send it to the other party


            //Create a Buffered Writer betwen proxy and remote
            BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

            // Create Buffered Reader from proxy and remote
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



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

            if(proxyToServerBR != null){
                proxyToServerBR.close();
            }

            if(proxyToServerBW != null){
                proxyToServerBW.close();
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
                sendNonCachedToClient(url);
            }
        }
    }
}




