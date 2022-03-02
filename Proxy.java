/*
 * Varjak Wolfe
 * 18325326
 * Advanced Computer Networks
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

public class Proxy {

    /*
     * HashTable with page URL as value and cached file
     * as the key.
     */
    static Hashtable<String, File> cachedPages = new Hashtable<>();

    /*
     * ArrayList for a dynamic-sized list of blocked sites.
     */
    static ArrayList<String> blockedPages = new ArrayList<>();;

    /*
     * ArrayList for a dynamic-sized list of currently-running threads
     */
    static ArrayList<Thread> currentThreads = new ArrayList<>();;

    //Create Proxy
    public Proxy(int port) {


    }

    public static void main(String[] args) {
        // Create an instance of Proxy and begin listening for connections
        Proxy myProxy = new Proxy(8080);
        myProxy.listen();
    }

}
