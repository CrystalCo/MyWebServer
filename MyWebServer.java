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
 * The returnErrorMessage is designed after Moeller and Schwartzbach's WebServer's "errorReport". 
 * I considered removing it from my code, but I found it helpful for debugging and hope this helps
 * the grader in debugging as well.
 * Both sendMeTheFile and headerAndFiles methods were modelled after Moeller and Schwartzbach's WebServer's sendFile method
 * and main method.
 * 
 * Citation:
 * Moeller, Anders, and Michael I. Schwartzbach. “A Web Server in 150 Lines.” Java and WWW / A Web Server in 150 Lines, Feb. 2002, cs.au.dk/~amoeller/WWW/javaweb/server.html.
 * ----------------------------------------------------------*/
import java.io.*;    // importing all files under Java's input output (io) library
import java.net.*;   // Java's networking libraries
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
        OutputStream bufOut = null;	// output stream in bytes.  Needed to send files
        PrintStream printOut = null;    // prints the actual data to the web client

        // this time we must parse out the Output stream from the Print Output Stream.
        
        try {
            in  = new BufferedReader(new InputStreamReader(sock1.getInputStream()));
            bufOut = new BufferedOutputStream(sock1.getOutputStream()); // The web client receives the buffered output stream (all the bytes!)
            printOut = new PrintStream(bufOut); // Will make it look pretty aka interpret the bytes into characters humans can read
             // wait for request

            String filename;    // will store the parsed filename
            String sockdata;    // will carry the data being bent by the webclient 
            String request;     // CL Header Request

           
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
                        returnErrorMessage(printOut, sock1, "403", "Forbidden", "Cease and desist, hacker!  You know very well you do not have permission to access this URL.");
                        break;
                    } else if (request.contains("favicon")) {
                        // Ignore those pesky favicon requests
                        return;
                    } else {
                        // parse request into a filename
                        filename = request.substring(5);    // gets rid of the "GET ", whitespace, and forward slash   
                        String[] parseGetRequest = filename.split(" ");
                        filename = parseGetRequest[0];  // gets rid of the "HTTP 1/1" part

                        String pathname;    // Will add the current directory's pathname to the filename being requested.
                        File webserverFile; // turns the filename from an ordinary string, to an actual file to return! Cool, right?

                        if (filename.contains("fake-cgi")) {
                            // parses FORM arguments submitted from addnum.html
                            String[] parameters;    // will store the data being passed in, specifically the queried parameters
                            String name;
                            String num1;
                            String num2;
                            int total;

                            pathname = localPath + "/addnum.html";  // return to same html page. Later maybe change to dynamically recreate the page?

                            parameters = filename.split("\\?"); // separates filename from parameters
                            parameters = parameters[1].split("&");  // seperates parameters 

                            // separate values from parameters
                            name = parameters[0].split("=")[1];     
                            num1 = parameters[1].split("=")[1];
                            num2 = parameters[2].split("=")[1];

                            total = Integer.parseInt(num1) + Integer.parseInt(num2);
                            System.out.println("Int total: " + total);

                            // will return the same addnum.html page with the answer and total
                            readAddNumFile(name, total);

                        } else {
                            // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                            pathname = localPath + "/" + filename;
                        }
                        
                        // LOOK IN DIRECTORY WHERE THE WEBSERVER IS RUNNING FOR THAT FILE
                        webserverFile = new File(pathname);
                        

                        if (webserverFile.isDirectory()) {
                            // returns indexed list of current directory files and folders
                            // unforunately, it does a poor job of returning subdirectories
                            pathname = pathname + "index.html";
                            webserverFile = new File(pathname);                           
                            readFiles();
                        }
                        // I exctracted this method out because I was testing out whether I should
                        // make another version similar to this method and use it on the fake-cgi file requests.
                        // The plan I tried was instead of hard-coding the addnum.html file back in with the answer,
                        // I wanted to append the answer to the current addnum.html file and return that.
                        // Another idea I had was to create a <div> within the addNum.html that an ID of "answer",
                        // then use JavaScript or jQuery to append the answer to the ID, but after asking the forum
                        // to double-check if that was okay, I saw that the addnum file will be generated by the grader,
                        // therefore this didn't work either.  Since I spent so much time on my two approaches above, I 
                        // left headerAndFiles as is below, in it's own method, but I wanted to explain why that was.
                        headerAndFiles(bufOut, printOut, pathname, webserverFile);
                        
                    }
                } 
                // else { System.out.println(sockdata);  }      // Returns the rest of the request headers that don't start with GET
                bufOut.flush ();   // browser receives my data output/contents of file 
            }        
            sock1.close();	// closes this socket connection but not the server
        } catch (IOException err2) {
            System.out.println("Connetion reset. Listening again...");
            System.out.println(err2);
        }
    }

	private void headerAndFiles(OutputStream bufOut, PrintStream printOut, String pathname, File webserverFile) {
		try {
		   // open the file and/or headers.  Afterwards, send over the data content in the return file back
		    InputStream file = new FileInputStream(webserverFile);  // opens the file & the parent directory.  It opens the subdirectories if you specifically call a file within the subdirectory, but otherwise it will not open a subdirectory with its list of files within in.
		    String contentType = getContentType(pathname);

		    printOut.print("HTTP/1.1 200 OK" + "\r\n" +
		    "Server: FileServer 1.0" + "\r\n" + 
		    "Content-Length: " + webserverFile.length() + "\r\n" +
		    "Connection: keep-alive" + "\r\n" + 
		    "Content-Type: " + contentType + "\r\n\r\n");
		    sendMeTheFile(file, bufOut);    // sends out the selected file
		    System.out.println("200 OK");
        } catch (FileNotFoundException e) {
            returnErrorMessage(printOut, sock1, "404", "FILE NOT FOUND", "Sorry, we can't find the file you're looking for");
        }
    }

    static void readFiles() {
        File fileInCurrentDir = new File("./"); // necessary to read in the new files by creating new files to put the current files in, in order to return the files
        File[] directoriesAndFilesList = fileInCurrentDir.listFiles();   // list out the entire collections of files under the current directory

        try {
            PrintStream outputStream = new PrintStream(new File("index.html"));
            PrintStream console = System.out; // we must save the usual System.out command somewhere else in the meantime
            System.setOut(outputStream);    // Now we can use System.out to return as a file for the web client to read
            System.out.println("<html> <head> <title> MyWebServer </title> </head> <body>");
            System.out.println("<h1>Crystal's WebServer Index</h1>");
            for (int i = 0; i < directoriesAndFilesList.length; i++) {
                if (directoriesAndFilesList[i].isDirectory()) {
                    // I couldn't figure out how to do this recursively.  When I tried, I would get an error that recursive calls couldn't be made on threads.  After failing miserably in attempting to thread off a new thread to do the work recursively, I gave up since I ran out of time.
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
        // hard codes addnum.html file back in with the answer to the submitted inputs.
        // As stated above, I did not succeed in finding a more elegant solution in a timely manner.
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

    static void returnErrorMessage(PrintStream printOut, Socket connection, String errorNumber, String nameOfError, String msg) {
        // Not necessary, but helpful in debugging!
        printOut.print("HTTP/1.0 " + errorNumber + " " + nameOfError + "\r\n" + "\r\n" +
                   "<!DOCTYPE HTML> \r\n" +"<TITLE>" + errorNumber + " " + nameOfError + "</TITLE>\r\n" +
                   "</HEAD><BODY>\r\n" +
                   "<H1>" + nameOfError + "</H1>\r\n" + msg + "<P>\r\n" +
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
        // Only used the main types to be used, and catch those pesky favicons
        if (path.endsWith(".txt") || path.endsWith(".java"))   return "text/plain";
        else if (path.endsWith(".html"))    return "text/html";
        else if (path.endsWith(".ico"))     // For favicon icon
            return "image/x-icon";  // even when I added in a favicon, the request would keep coming in!  It was very annoying, so ultimately I filtered them from ever finishing the ListenWorker thread, and thus no longer need this content-type here, since it isn't being used anymore, but I left it for pedagogical reasons.
        else if (path.contains("fake-cgi")) {
            System.out.println("Caught fake-cgi request!");
            return "text/html";
        }
        // default:
        else return "text/plain";
    }
}


// aka MyListener
public class MyWebServer {
    public static boolean controlSwitch = true;
    public static void main(String args[]) throws IOException {
        Socket sock1;       // creates client socket
        int q_len = 6;	    // # of requests for operating system to queue up
        int port = 2540;    // Port number for which localhost should bind to
        String localDirPath = System.getProperty("user.dir");    // Grabs users current directory

        ServerSocket serverSock = new ServerSocket(port, q_len);  // creates new server socket using port number and number of requests

        System.out.println("Clark Elliott's MyWebServer Listener 1.8 starting up, listening at port " + port + ".\n"); // Prints out to terminal running InetServer

        while(controlSwitch) {
            sock1 = serverSock.accept();	// wait for the next client connection
            new ListenWorker(sock1, localDirPath).start();	// Uncomment to see shutdown bug:
            // try{Thread.sleep(10000);} catch(InterruptedException ex) {System.out.println(ex);}
        }
    }
}

