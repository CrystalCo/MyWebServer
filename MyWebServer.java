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
 * After recording the request and updating my code to match a similar response, 
 * I compile my code again and from here on out enter my local pathname for which MyWebServer
 * should read from:
 * > javac *.java
 * > java MyWebServer 2540 /Users/crystalcontreras/Desktop/DePaul/2019Autumn/Distributed_Systems_CSC435/MyWebServer
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
import java.util.*;

class ListenWorker extends Thread {
    Socket sock1;
    String localPath;

    ListenWorker (Socket s, String path) {	
        sock1 = s;
        localPath = path;
    } 

    public void run() {
        BufferedReader in = null;
        OutputStream out = null;	
        PrintStream pout = null;

        
        try {
            in  = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
            out = new BufferedOutputStream(sock1.getOutputStream());
            pout = new PrintStream(out);
             // wait for request

            String filename;
            String sockdata; 
            String request;// CL Header Request

            // log(sock1, request);
           
            while (true) {
                sockdata = in.readLine (); // CL Header Request
                // save GET request:
                if (sockdata.startsWith("GET")) {
                    // Proccess request by extracting the GET request line
                    request = sockdata;
                    System.out.println("GET REQUEST: " + request);

                    if (request.contains("..")) {
                        // LATER REPLACE WITH:          if (req.indexOf("..")!=-1 || req.indexOf("/.ht")!=-1 || req.endsWith("~")) {
                        // if client is trying to access directories outside this one, close the socket and return nothing to those hackers!
                        errorReport(pout, sock1, "403", "Forbidden", "You don't have permission to access the requested URL.");
                        break;
                    } else {
                        // parse filename
                        filename = request.substring(5);    
                        String[] parseGetRequest = filename.split(" ");
                        filename = parseGetRequest[0];
                        System.out.println("PARSED FILENAME: " + filename);
                        System.out.println("Filename.length: " + filename.length());
                        
                        // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                        // String pathname = "/Users/crystalcontreras/Desktop/DePaul/2019Autumn/Distributed_Systems_CSC435/MyWebServer/" + filename;
                        String pathname = localPath + "/" + filename;
                        System.out.println("PATHNAME: " + pathname);

                        // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                        File webserverFile = new File(pathname);

                        if (webserverFile.isDirectory() || filename.length() < 2) {
                            pathname = pathname + "index.html";
                            webserverFile = new File(pathname);
                            readFiles();
                        }

                        try {
                           // OPEN THE FILE (&/OR DIRECTORY) 
                                // AND (SEND THE CONTENTS OF THE DATA IN THE FILE BACK TO THE WEB SERVER OVER THE SOCKET)
                            InputStream file = new FileInputStream(webserverFile);  // opens the file & or directory
                            System.out.println("guess content type: " + guessContentType(pathname));

                            pout.print("HTTP/1.0 200 OK" + "\r\n" +
                            // "Server: FileServer 1.0" + "\r\n" + 
                            "Content-Length: 200" + "\r\n" +
                            // "Connection: close \r\n" + 
                            "Content-Type: " + guessContentType(pathname) + "\r\n\r\n");
                            sendFile(file, out); // SEND RAW FILE
                            log(sock1, "200 OK");
                        } catch (FileNotFoundException e) {
                            errorReport(pout, sock1, "404", "FILE NOT FOUND", "The requested URL was not found on this server");
                        }
                    }
                } 
                // else { System.out.println(sockdata);  }
                out.flush ();   // browser receives my data output/contents of file 
            }        
            sock1.close();	// closes this socket connection but not the server
        } catch (IOException err2) {
            System.out.println("Connetion reset. Listening again...");
            System.out.println(err2);
        }
    }

    static void readFiles() {
        String filedir;
        // Create a file object for your root directory      
        // For Unix:
        File f1 = new File ( "./" ) ;
        // Get all the files and directory under your diretcory
        File[] strFilesDirs = f1.listFiles();
        
        for (int i = 0; i < strFilesDirs.length; i ++) {
            if (strFilesDirs[i].isDirectory())   System.out.println("Directory: " + strFilesDirs[i]);
            else if (strFilesDirs[i].isFile( ))  System.out.println ("File: " + strFilesDirs[i] +  " (" + strFilesDirs[i].length( ) + ")");
        }
    }

    static void log(Socket connection, String msg) {
        System.err.println(" [" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + "] " + msg);
    }

    static void errorReport(PrintStream pout, Socket connection, String code, String title, String msg) {
        pout.print("HTTP/1.0 " + code + " " + title + "\r\n" +
                   "\r\n" +
                   "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                   "<TITLE>" + code + " " + title + "</TITLE>\r\n" +
                   "</HEAD><BODY>\r\n" +
                   "<H1>" + title + "</H1>\r\n" + msg + "<P>\r\n" +
                   "<HR><ADDRESS>FileServer 1.0 at " + 
                   connection.getLocalAddress().getHostName() + 
                   " Port " + connection.getLocalPort() + "</ADDRESS>\r\n" +
                   "</BODY></HTML>\r\n");
        log(connection, code + " " + title);
        try {
            connection.close();
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    static void sendFile(InputStream file, OutputStream out) {
        try {
            byte[] buffer = new byte[1000];
            while (file.available()>0) 
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException e) { System.err.println(e); }
    }
    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) 
            return "text/html";
        else if (path.endsWith(".txt") || path.endsWith(".java")) 
            return "text/plain";
        else if (path.endsWith(".gif")) 
            return "image/gif";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else    
            return "text/plain";
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
    public static void main(String args[]) throws IOException {
        // port http://localhost:2540
        int q_len = 6;	// # of requests for operating system to queue up
        // int port = 2540; 
        Socket sock1; // creates client socket
        // String portNum = args[0];
        // String pathname = args[1];

        // read arguments
        if (args.length!=2) {
            System.out.println("Usage: java FileServer <port> <wwwhome>");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[0]);
        String wwwhome = args[1];

        System.out.println("First arg: " + port + " . Second arg: " + wwwhome);

        
        ServerSocket serverSock = new ServerSocket(port, q_len);  // creates new server socket using port number and number of requests

        System.out.println("Clark Elliott's MyWebServer Listener 1.8 starting up, listening at port " + port + ".\n"); // Prints out to terminal running InetServer

        while(controlSwitch) {
            sock1 = serverSock.accept();	// wait for the next client connection
            new ListenWorker(sock1, wwwhome).start();	// Uncomment to see shutdown bug:
            // try{Thread.sleep(10000);} catch(InterruptedException ex) {}
        }
    }
}

