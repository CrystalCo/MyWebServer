/***  --------------------------------------------------------
 * 1. Name: CRYSTAL A. CONTRERAS 
 *    Date: 10/6/2019
 * 2. Java version used:
 * openjdk version "1.8.0_222"
 * OpenJDK Runtime Environment (AdoptOpenJDK)(build 1.8.0_222-b10)
 * OpenJDK 64-Bit Server VM (AdoptOpenJDK)(build 25.222-b10, mixed mode)
 * 
 * 3. Precise command-line compilation examples / instructions:
 * > javac *.java
 * 
 * 4. Precise examples / instructions to run this program:
 * TO RUN TELNET:
 * I installed the telnet application using homebrew:
 * > brew install telnet
 * 
 * After it finished installing, I tested it by entering:
 * > telnet condor.depaul.edu 80
 * 
 * This is the response I received:
 * 
        * Crystals-MacBook:MyWebServer crystalcontreras$ telnet condor.depaul.edu 80
        * Trying 216.220.180.150...
        * Connected to condor.depaul.edu.
        * Escape character is '^]'.
        * ^] 
        * HTTP/1.1 400 Bad Request
        * Date: Sun, 06 Oct 2019 19:20:34 GMT
        * Server: Apache
        * Content-Length: 226
        * Connection: close
        * Content-Type: text/html; charset=iso-8859-1
        * 
        * <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
        * <html><head>
        * <title>400 Bad Request</title>
        * </head><body>
        * <h1>Bad Request</h1>
        * <p>Your browser sent a request that this server could not understand.<br />
        * </p>
        * </body></html>
        * Connection closed by foreign host.
 * 
 * Apparently, I was not the only student to get this response.
 *
 * Next, I compiled and ran MyWebServer:
 * > javac *.java
 * > java MyWebServer
 * 
 * On the Firefox browser's url, I entered:     http://localhost:2540/TEST
 * 
 * This is the response I received on MyWebServer:
 * 
 * Crystals-MacBook:MyWebServer crystalcontreras$ java MyWebServer
 * Clark Elliott's MyWebServer Listener 1.8 starting up, listening at port 2540.
 * 
 * GET /TEST HTTP/1.1
 * Host: localhost:2540
 * User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:69.0) Gecko/20100101 Firefox/69.0
 * Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*\/*;q=0.8         <-- inserted backslash before the forwardslash to prevent comment section from closing out
 * Accept-Language: en-US,en;q=0.5
 * Accept-Encoding: gzip, deflate
 * Connection: keep-alive
 * Upgrade-Insecure-Requests: 1
 * 
 * 
 * 5. List of files needed for running the program, all of which are in the same directory.
 * a. checklist-mywebserver.html
 * b. MyWebServer.java
 * c. http-streams.txt
 * d. serverlog.txt
 * 
 * 5. Notes:
 * ----------------------------------------------------------*/
import java.io.*;    // importing all files under Java's input output (io) library
import java.net.*;   // net stands for Java's networking libraries

class ListenWorker extends Thread {
    Socket sock1;

    ListenWorker (Socket s) {	
        sock1 = s; 	
    } 

    public void run() {
        PrintStream out = null;	
        BufferedReader in = null;
        
        try {
            in  = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
            out = new PrintStream(sock1.getOutputStream());
            String filename;
            
            while (true) {
                // WEBSER ACCEPTS A STRING OF FILENAME
                // Read in the name of a file (i.e. dog.txt)
                // CAPTURE THE FILENAME
                // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                // OPEN THE FILE (&/OR DIRECTORY) 
                    // AND (SEND THE CONTENTS OF THE DATA IN THE FILE BACK TO THE WEB SERVER OVER THE SOCKET)
                // SEND BACK THE APPROPRIATE HTTP HEADERS TO THE BROWSER
                    // (..., Content-Length, MIME type header, Content-Type, \n, \n, )
                    // Goal: Your web server must correctly return requests for files with extensions of .txt, and .html [and also .java which are treated as the same as .txt]. This means that it must return the correct MIME headers (That is, the Content-type [followed by two cr/lf], and Content-length headers), as well as the data. This is a server that operates on static data.
                    //  Copy your MyListener.java source into a file called MyWebServer.java.
                    
                // THEN THE WEB BROWSER WILL RECEIVE THE CONTENTS OF THE FILE FROM YOUR PROGRAM 
                // CLOSE CONNECTION DONE
                filename = in.readLine ();
                // System.out.println("Looking up " + filename);
                // printRemoteAddress(filename, out);
                if (filename != null) System.out.println(filename);
                System.out.flush ();
            }

            // sock1.close();	// closes this socket connection but not the server
        } catch (IOException err2) {
            System.out.println("Connetion reset. Listening again...");
            System.out.println(err2);
        }
    }

    static void printRemoteAddress(String name, PrintStream out) {
        try {
            out.println("Looking up " + name + "...");
            InetAddress machine = InetAddress.getByName(name);
            out.println("Host name: " + machine.getHostName());  
            out.println("Host IP: " + toText(machine.getAddress()));  
        } catch(UnknownHostException err3) {
            out.println("Failed in attempt to look up " + name);
        }
    }

    static String toText(byte ip[]) { 
        // turns IP address integers into string
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < ip.length; ++i) {
            if (i > 0) result.append(".");
                result.append(0xff & ip[i]);
            }
        return result.toString();
    }
}


// aka MyListener
public class MyWebServer {
    public static boolean controlSwitch = true;
    public static void main(String a[]) throws IOException {
        // port http://localhost:2540
        int q_len = 6;	// # of requests for operating system to queue up
        int port = 2540; 
        Socket sock1; // creates client socket
        
        ServerSocket serverSock = new ServerSocket(port, q_len);  // creates new server socket using port number and number of requests

        System.out.println("Clark Elliott's MyWebServer Listener 1.8 starting up, listening at port " + port + ".\n"); // Prints out to terminal running InetServer

        while(controlSwitch) {
            sock1 = serverSock.accept();	// wait for the next client connection
            new ListenWorker(sock1).start();	// Uncomment to see shutdown bug:
            // try{Thread.sleep(10000);} catch(InterruptedException ex) {}
        }
    }
}

