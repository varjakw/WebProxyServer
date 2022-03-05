/*
 * Varjak Wolfe
 * 18325326
 * Advanced Computer Networks
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class Proxy implements Runnable {
    private ServerSocket socketS;
    private boolean running = true;
    //HashTable with page URL as value and cached file as the key.
    static Hashtable<String, File> cachedPages;
    //ArrayList for a dynamic-sized list of blocked sites.
    static ArrayList<String> blockedPages;
    //ArrayList for a dynamic-sized list of currently-running threads
    static ArrayList<Thread> currentThreads;

    //Create Proxy
    public Proxy(int port) throws IOException, ClassNotFoundException {
        cachedPages = new Hashtable<>();
        blockedPages = new ArrayList<>();
        currentThreads = new ArrayList<>();
        new Thread(this).start();

        //cache from txt into hashtable
       File cache = new File("cache.txt");
       FileInputStream fis1 = new FileInputStream(cache);
        ObjectInputStream ois1 = new ObjectInputStream(fis1);
        cachedPages = (Hashtable<String, File>)ois1.readObject();
        fis1.close();
        ois1.close();

        //blocked pages from txt into arraylist
        File blocked = new File("blocked.txt");
        FileInputStream fis2 = new FileInputStream(blocked);
        ObjectInputStream ois2 = new ObjectInputStream(fis2);
        blockedPages = (ArrayList<String>) ois2.readObject();
        fis2.close();
        ois2.close();

        //make server
        socketS = new ServerSocket(port);
        System.out.println("Waiting...");
        running = true;
    }

    //listen for new connections
    public void listen() throws IOException {
        while(running = true){
            Socket socket = socketS.accept();
            Thread thread = new Thread(new Handler(socket));
            currentThreads.add(thread);
            thread.start();
        }

    }

    public void shutdown() throws IOException {
        System.out.println("Shutting down...");
        running = false;

        //cache
        FileOutputStream fos1 = new FileOutputStream("cache.txt");
        ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
        oos1.writeObject(cachedPages);
        oos1.close();
        fos1.close();

        //blocked pages
        FileOutputStream fos2 = new FileOutputStream("blocked.txt");
        ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
        oos2.writeObject(blockedPages);
        oos2.close();
        fos2.close();



    }

    public static void main(String[] args) {
        // Create an instance of Proxy and begin listening for connections
        Proxy myProxy = new Proxy(8080);
        myProxy.listen();
    }

    @Override
    public void run() {

    }
}
