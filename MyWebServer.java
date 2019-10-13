/***  --------------------------------------------------------
 * 1. Name: CRYSTAL A. CONTRERAS 
 *    Date: 10/12/2019
 * 2. Java version used:
 * openjdk version "1.8.0_222"
 * OpenJDK Runtime Environment (AdoptOpenJDK)(build 1.8.0_222-b10)
 * OpenJDK 64-Bit Server VM (AdoptOpenJDK)(build 25.222-b10, mixed mode)
 * 
 * 3. Precise command-line compilation examples / instructions:
 * > javac *.java
 * 
 * 4. Precise examples / instructions to run this program:
 * > java MyWebServer
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
    String localPath;   // the directory path for which MyWebServer is running

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

           
            while (true) {
                sockdata = in.readLine (); // CL Header Request
                
                if (sockdata == null) {
                    // Fixes the "Exception in thread 'Thread-0' java.lang.NullPointerException" error I kept getting
                    return;
                }

                // Only processes lines that start with GET
                if (sockdata.startsWith("GET")) {
                    // Proccess request by extracting the GET request line
                    request = sockdata;

                    if (request.contains("..") || request.contains("~")) {
                        // if client is trying to access directories outside this one, close the socket and return nothing to those hackers!
                        errorReport(pout, sock1, "403", "Forbidden", "You don't have permission to access the requested URL.");
                        break;
                    } else if (request.contains("favicon")) {
                        // Ignore those pesky favicon requests
                        return;
                    } else {
                        // parse request into a filename
                        filename = request.substring(5);    
                        String[] parseGetRequest = filename.split(" ");
                        filename = parseGetRequest[0];
                        System.out.println("\n" + "PARSED FILENAME: " + filename);

                        String pathname;
                        File webserverFile;

                        if (filename.contains("fake-cgi")) {
                            // parses FORM arguments submitted from addnum.html
                            String[] parameters;
                            String name;
                            String num1;
                            String num2;
                            int total;

                            pathname = localPath + "/addnum.html";  // return to same html page. Later maybe change to dynamically recreate the page?
                            System.out.println("PATHNAME: " + pathname);

                            parameters = filename.split("\\?"); // separates filename from parameters
                            parameters = parameters[1].split("&");  // seperates parameters 
                            name = parameters[0].split("=")[1];
                            num1 = parameters[1].split("=")[1];
                            num2 = parameters[2].split("=")[1];
                            System.out.println("NAME: " + name);
                            System.out.println("NUM1: " + num1);
                            System.out.println("NUM2: " + num2);

                            total = Integer.parseInt(num1) + Integer.parseInt(num2);
                            System.out.println("Int total: " + total);

                            // either return to homepage, make a new addnum txt file, 
                            // or append answer to the addnum html file.
                            readAddNumFile(name, total);

                        } else {
                            // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                            pathname = localPath + "/" + filename;
                            System.out.println("PATHNAME: " + pathname);
                        }
                        
                        // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                        webserverFile = new File(pathname);
                        

                        if (webserverFile.isDirectory()) {
                            // returns indexed list of current directory files and folders
                            System.out.println("This is a directory!");
                            pathname = pathname + "index.html";
                            webserverFile = new File(pathname);
                            System.out.println("WEBSERVER FILE: " + webserverFile);
                           
                            readFiles();
                        }
                         
                        headerAndFiles(out, pout, pathname, webserverFile);
                        
                    }
                } 
                // else { System.out.println(sockdata);  }      // Returns the rest of the request headers that don't start with GET
                out.flush ();   // browser receives my data output/contents of file 
            }        
            sock1.close();	// closes this socket connection but not the server
        } catch (IOException err2) {
            System.out.println("Connetion reset. Listening again...");
            System.out.println(err2);
        }
    }

	private void headerAndFiles(OutputStream out, PrintStream pout, String pathname, File webserverFile) {
		try {
		   // open the file and/or headers.  Afterwards, send over the data content in the return file back
		    InputStream file = new FileInputStream(webserverFile);  // opens the file & or directory
		    String contentType = getContentType(pathname);

		    pout.print("HTTP/1.1 200 OK" + "\r\n" +
		    "Server: FileServer 1.0" + "\r\n" + 
		    "Content-Length: " + webserverFile.length() + "\r\n" +
		    "Connection: keep-alive" + "\r\n" + 
		    "Content-Type: " + contentType + "\r\n\r\n");
		    sendMeTheFile(file, out);    // sends out the selected file
		    System.out.println("200 OK");
        } catch (FileNotFoundException e) {
            errorReport(pout, sock1, "404", "FILE NOT FOUND", "The requested URL was not found on this server");
        }
    }

    static void readFiles() {
        File fileInCurrentDir = new File("./");
        File[] directoriesAndFilesList = fileInCurrentDir.listFiles();   // list out the entire collections of files under the current directory

        try {
            PrintStream outputStream = new PrintStream(new File("index.html"));
            PrintStream console = System.out; // we must save the usual System.out command somewhere else in the meantime
            System.setOut(outputStream);    // Now we can use System.out to return as a file for the web client to read
            System.out.println("<html> <head> <title> MyWebServer </title> </head> <body>");
            System.out.println("<h1>Crystal's WebServer Index</h1>");
            for (int i = 0; i < directoriesAndFilesList.length; i++) {
                if (directoriesAndFilesList[i].isDirectory()) {
                    System.out.println("<a href=\"" + directoriesAndFilesList[i] + "/\">" + directoriesAndFilesList[i] + "/</a><br> ");
                } else if (directoriesAndFilesList[i].isFile()) {
                    System.out.println("<a href=\"" + directoriesAndFilesList[i] + "\">" + directoriesAndFilesList[i] + "</a><br> ");
                }
            }
            
            System.out.println("</body> </html>");

            System.setOut(console); // restore System.out to its rightful place
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void readAddNumFile(String name, int total) {
        try {
            PrintStream outputStream = new PrintStream(new File("addnum.html"));
            PrintStream console = System.out;
            System.setOut(outputStream);
            System.out.println("<html> <head> <title>CSC435 AddNum</title> </head> <body>");
            System.out.println("<h1>Crystal's AddNum</h1>");
            System.out.println("<form method=\"GET\"action=\"http://localhost:2540/cgi/addnums.fake-cgi\"> <p>Enter your name and two numbers:</p> <input type=\"text\" name=\"person\" size=20 value=\"YourName\"> <input type=\"text\" name=\"num1\" size=5 value=\"4\"> <br> <input type=\"text\" name=\"num2\" size=5 value=\"5\"> <br> <input type=\"submit\" value=\"Submit Numbers\"> </form>");
            System.out.println("<h2> Bonjour, " + name + "!</h2> <p> Your total is " + String.valueOf(total) );
            System.out.println("</body> </html>");
            System.setOut(console); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
        try {
            connection.close();
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    static void sendMeTheFile(InputStream inFile, OutputStream streamout) {
        try {
            byte[] buf = new byte[1000];    // more buffer space!
            while (inFile.available()>0)    // while more space exists
            streamout.write(buf, 0, inFile.read(buf));      // write out the file to the buffer
        } catch (IOException e) { System.err.println(e); }  // catch any errors and print them to the system.
    }

    static String getContentType(String path) {
        if (path.endsWith(".txt") || path.endsWith(".java"))   return "text/plain";
        else if (path.endsWith(".html"))    return "text/html";
        else if (path.endsWith(".ico"))     // For favicon icon
            return "image/x-icon";
        else if (path.contains("fake-cgi")) {
            System.out.println("Caught fake-cgi request!");
            return "text/html";
        }
        else
            return "text/plain";
    }
}


// aka MyListener
public class MyWebServer {
    public static boolean controlSwitch = true;
    public static void main(String args[]) throws IOException {
        Socket sock1;       // creates client socket
        int q_len = 6;	    // # of requests for operating system to queue up
        int port = 2540;    // Port number for which localhost should bind to
        String wwwhome = System.getProperty("user.dir");    // Grabs users current directory

        ServerSocket serverSock = new ServerSocket(port, q_len);  // creates new server socket using port number and number of requests

        System.out.println("Clark Elliott's MyWebServer Listener 1.8 starting up, listening at port " + port + ".\n"); // Prints out to terminal running InetServer

        while(controlSwitch) {
            sock1 = serverSock.accept();	// wait for the next client connection
            new ListenWorker(sock1, wwwhome).start();	// Uncomment to see shutdown bug:
            // try{Thread.sleep(10000);} catch(InterruptedException ex) {System.out.println(ex);}
        }
    }
}

