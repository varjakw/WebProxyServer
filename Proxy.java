/*
 * Varjak Wolfe
 * 18325326
 * Advanced Computer Networks
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

public class Proxy implements Runnable {
    private ServerSocket socketS;
    private boolean running = true;
    //HashTable with page URL as value and cached file as the key.
    static Hashtable<String, File> cache;
    //Hashtable for a list of blocked sites.
    static Hashtable<String, String> blocked;
    //ArrayList for a dynamic-sized list of currently-running threads
    static ArrayList<Thread> currentThreads;

    //Create Proxy
    public Proxy(int port) throws ClassNotFoundException {
        cache = new Hashtable<>();
        blocked = new Hashtable<>();
        currentThreads = new ArrayList<>();
        new Thread(this).start();

        try{
            File cachedPages = new File("cache.txt");
            File blockedPages = new File("blocked.txt");
            if(!cachedPages.exists()){
                cachedPages.createNewFile();
            }else{
                FileInputStream fileInputStream = new FileInputStream(cachedPages);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                cache = (Hashtable<String,File>)objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            }
            if(!blockedPages.exists()){
                blockedPages.createNewFile();
            }else{
                FileInputStream fileInputStream = new FileInputStream(blockedPages);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                blocked = (Hashtable<String,String>)objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            }
        } catch (IOException e) {
            System.out.println("Error finding old cache");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException");
        }

        //make server
        try {
            socketS = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error making server socket");
        }
        System.out.println("Waiting...");
        running = true;
    }

    //listen for new connections
    public void listen() {
        try{
            while(running = true){
                Socket socket = socketS.accept();
                Thread thread = new Thread(new Handler(socket));
                currentThreads.add(thread);
                thread.start();
            }
        }catch(IOException e){
            System.out.println("Error with thread listening");
        }

    }

    public void shutdown() throws InterruptedException {
        System.out.println("Shutting down...");
        running = false;
        try{
            //cache
            FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(cache);
            objectOutputStream.close();
            fileOutputStream.close();

            //blocked pages
            FileOutputStream fileOutputStream1 = new FileOutputStream("cachedSites.txt");
            ObjectOutputStream objectOutputStream1 = new ObjectOutputStream(fileOutputStream);
            objectOutputStream1.writeObject(blocked);
            objectOutputStream1.close();
            fileOutputStream1.close();

            for(Thread thread: currentThreads){
                if(thread.isAlive()){
                    thread.join();
                }
            }

            socketS.close();
            System.out.println("Socket closed");
        } catch(IOException e){
            System.out.println("Error closing socket");
        }
    }

    public static File getFromCache(String url){
        File page = cache.get(url);
        //return cached file/page if it is cached
        return page;
    }

    public static void addToCache(String url, File file){
        cache.put(url, file);
    }

    ////////////////
    public static boolean blocked (String url) {
        if(blocked.get(url) != null){
            return true;
        } else{
            return false;
        }
    }
    /////////////////////
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String input;

        while(running){
            System.out.println("Please type:\n 1: A site you want blocked \n 2: \"cache\" to view cache \n 3: \"blocked\" to view a list of blocked sites \n 4: \"shutdown\" to shut the server down");
            input = scanner.nextLine();

            if(input.equals("cache")){
                System.out.println("\n Cached Sites");
                for(String cachedSite : cache.keySet()){
                    System.out.println(cachedSite);
                }
                System.out.println();
            }
            else if(input.equals("blocked")){
                System.out.println("\n Blocked Sites: ");
                for(String blockedSite : blocked.keySet()){
                    System.out.println(blockedSite);
                }
                System.out.println();
            }
            else if(input.equals("shutdown")){
                try {
                    shutdown();
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException");
                }
            }
            else {
                blocked.put(input, input);
                System.out.println("\n You have blocked " + input);
            }
        }

    }

    public static void main(String[] args) throws ClassNotFoundException {
        Proxy proxy = null;
        proxy = new Proxy(8080);
        proxy.listen();
        //listen for connections
    }
}
