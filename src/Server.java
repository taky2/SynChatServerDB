import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.sql.*;


/***********************************************************************************
 * Server can be executed using this class or the UI class                         *
 ***********************************************************************************
 *** WARNING: Console mode does not work correctly PLEASE USE UI VERSION ONLY!!! ***
 ***********************************************************************************/

public class Server {

    private static int connectionUID; // each connection receives a unique ID
    private ArrayList<ClientThread> threadArray; // list of connected clients
    private SimpleDateFormat sdf; // time
    private int port; // port number for socket connection to listen on
    private boolean keepListening; // signal used to stop server or keep listening
    private ServerUI ui; // for use by UI

    /**
     *  To run as a console application just open a console window and:
     *   > java Server [portNumber]
     *
     *  If no port number specified default (8700) is used
     **/
    public static void main(String[] args) {

        // start server on port 8700 unless a PortNumber is specified
        int portNumber = 8700;
        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    } // end main

    /** polymorphic server constructor to specify port number **/
    public Server(int port) {
        this(port, null);
    }
    /** polymorphic server constructor to specify port number **/
    public Server(int port, ServerUI ui) {
        this.ui = ui; // to GUI or not to GUI
        this.port = port; // specify port
        sdf = new SimpleDateFormat("HH:mm:ss"); // specify date format
        threadArray = new ArrayList<ClientThread>(); // client list
    }

    /** Start Server: create and open server socket, then wait for requests to connect **/
    public void start() {
        // create a Derby database for chatlog
        System.out.println("Creating Chatlog Database");
        CreateChatlogDB();

        keepListening = true;
        try
        {
            ServerSocket serverSocket = new ServerSocket(port); // specify socket for server
            while(keepListening) // wait for connections while true
            {
                display("Server waiting for Clients on port " + port + ".");
                Socket socket = serverSocket.accept(); // accept connection
                if(!keepListening) // break if false
                    break;
                ClientThread clientThread = new ClientThread(socket);  // make thread for new client
                threadArray.add(clientThread); // save it in client list
                clientThread.start(); // start thread
            }
            // if keepListening is false close server and stop listening
            try {
                serverSocket.close();
                // then also close all active client threads in list
                for(int i = 0; i < threadArray.size(); ++i) {
                    ClientThread cThread = threadArray.get(i);
                    try {
                        cThread.sInputStream.close();
                        cThread.sOutputStream.close();
                        cThread.socket.close();
                    }
                    catch(IOException ioE) {}
                }
            }
            catch(Exception e) {
                display("Error closing the server and/or clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Error creating new ServerSocket: " + e + "\n";
            display(msg); // send error message to console/ui
        }
    }


    /** method called by UI to stop server **/
    protected void stop() {
        keepListening = false;
        // connect as client to exit statement
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {}
    }

    /** method to display event prompts to server (not for chat message) (*/
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg + "";

        // add message to chatlog database
        try
        {
            // Create a named constant for the URL.
            // NOTE: This value is specific for Java DB.
            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

            // Create a connection to the database.
            Connection DbConnection = DriverManager.getConnection(DB_URL);

            // Add to ChatTable
            addToChatlogTable(DbConnection, time);

            // Close the connection.
            DbConnection.close();
        } catch (Exception e)
        {
            System.out.println("Error Creating the Chatlog Table");
            System.out.println(e.getMessage());
        }

        // msg to user
        if(ui == null)
            System.out.println(time);
        else
            ui.appendEvent(time + "\n");
    }

    /**
     * synchronized method used to broadcast and sync all chat messages with all clients
     * ( message is added to the sender's clientThread message queue )
     **/
    private synchronized void broadcast(String message) {
        String time = sdf.format(new Date()); // add timestamp to message
        String messageFormat = time + " " + message;

        // add message to chatlog database
        try
        {
            // Create a named constant for the URL.
            // NOTE: This value is specific for Java DB.
            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

            // Create a connection to the database.
            Connection DbConnection = DriverManager.getConnection(DB_URL);

            // Add to ChatTable
            addToChatlogTable(DbConnection, messageFormat);

            // Close the connection.
            DbConnection.close();
        } catch (Exception e)
        {
            System.out.println("Error Creating the Chatlog Table");
            System.out.println(e.getMessage());
        }

        if(ui == null)
            System.out.print(messageFormat); // display message on console or in UI
        else
            ui.appendRoom(messageFormat + "\n"); // append message to chat room log

        // loop through client list in reverse order (easier to remove a client if not responsive)
        for(int i = threadArray.size(); --i >= 0;) {
            ClientThread clientThread = threadArray.get(i);
            // if msg to client fails remove client from active list
            if(!clientThread.sendMsg(messageFormat)) {
                threadArray.remove(i);
                display("Unresponsive client " + clientThread.username + " removed.");
            }
        }

    }

    /**
     * synchronized method used to broadcast and sync all chat messages with all clients
     * ( message is added to the sender's clientThread message queue )
     **/
    private synchronized void clientCast(String message) {
        //String messageFormat =  message + "\n";

        // add message to chatlog database
        try
        {
            // Create a named constant for the URL.
            // NOTE: This value is specific for Java DB.
            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

            // Create a connection to the database.
            Connection DbConnection = DriverManager.getConnection(DB_URL);

            // Add to ChatTable
            addToChatlogTable(DbConnection, message);

            // Close the connection.
            DbConnection.close();
        } catch (Exception e)
        {
            System.out.println("Error Creating the Chatlog Table");
            System.out.println(e.getMessage());
        }

        if(ui == null)
            System.out.print(message); // display message on console or in UI
        else
            //ui.appendRoom(message + "\n"); // append message to chat room log

        // loop through client list in reverse order (easier to remove a client if not responsive)
        for(int i = threadArray.size(); --i >= 0;) {
            ClientThread clientThread = threadArray.get(i);
            // if msg to client fails remove client from active list
            if(!clientThread.sendMsg(message)) {
                threadArray.remove(i);
                display("Unresponsive client " + clientThread.username + " removed.");
            }
        }

    }

    /** synchronized method used when clients logout from chatroom **/
    synchronized void remove(int id) {
        // scan the client list for client UID
        for(int i = 0; i < threadArray.size(); ++i) {
            ClientThread clientThread = threadArray.get(i);
            // if client UID is found in list, remove it
            if(clientThread.id == id) {
                threadArray.remove(i);
                return;
            }
        }
    }

    /** an instance of this thread will run for each connected client **/
    class ClientThread extends Thread {

        Socket socket; // socket to listen
        ObjectInputStream sInputStream; // incoming message stream
        ObjectOutputStream sOutputStream; // outgoing message stream
        int id; // UID for each client
        String username; // username of the client
        ChatMessage cMessage; // message received
        String date; // date client connects

        // constructor
        ClientThread(Socket socket) {
            id = ++connectionUID; // assign clientThread a UID
            this.socket = socket; // specify this socket

            System.out.println("Creating object I/O streams for new clientThread");
            // open streams for new user and assign username
            try
            {
                sOutputStream = new ObjectOutputStream(socket.getOutputStream());
                sInputStream = new ObjectInputStream(socket.getInputStream());
                username = (String) sInputStream.readObject(); // read username from inputStream
                display("<" + username + "> just connected."); // prompt when a new user connects to chat
                //clientCast("<" + username + "> just connected.");
            }
            catch (IOException e) {
                display("Error creating new I/O streams: " + e);
                return;
            }
            catch (ClassNotFoundException e) {} // required
            date = new Date().toString() + "\n"; // time & date of client connection
        }

        /**
         * continuous message handling here
         *   reads messages from the queue and schedules delivery to all clients connected to server
         **/
        public void run() {
            boolean keepGoing = true; // continue until
            while(keepGoing) {
                // read string (ChatMessage object)
                try {
                    cMessage = (ChatMessage) sInputStream.readObject();
                }
                catch (IOException ioe) {
                    display(username + " Exception reading Streams: " + ioe);
                    break;
                }
                catch(ClassNotFoundException e) { break; } // required
                String message = cMessage.getMessage(); // get message from ChatMessage object (assign to message string)
                // type of response depends on type of message received
                switch(cMessage.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast("<" + username + ">: " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.USERLIST:
                        sendMsg("\n------------------------------------------------------------------------\n"
                                + "List of the users currently connected at " + sdf.format(new Date()) + "\n");
                        // scan current list of users connected
                        for(int i = 0; i < threadArray.size(); ++i) {
                            ClientThread clientThread = threadArray.get(i);
                            sendMsg((i + 1) + ") <" + clientThread.username + ">: connected since " + clientThread.date);
                        }
                        sendMsg("------------------------------------------------------------------------\n");
                        /**
                        try
                        {
                            // Create a named constant for the URL.
                            // NOTE: This value is specific for Java DB.
                            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

                            // Create a connection to the database.
                            Connection DbConnection = DriverManager.getConnection(DB_URL);

                            // Reference Tables and print
                            viewUserTable(DbConnection);

                            // Close the connection.
                            DbConnection.close();
                        } catch (Exception e)
                        {
                            System.out.println("Error Creating the Chatlog Table");
                            System.out.println(e.getMessage());
                        }
                        **/
                        break;
                    case ChatMessage.HISTORY:
                        // RETRIEVE INFO FROM DB HERE TO SHOW HISTORY
                        try
                        {
                            // Create a named constant for the URL.
                            // NOTE: This value is specific for Java DB.
                            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

                            // Create a connection to the database.
                            Connection DbConnection = DriverManager.getConnection(DB_URL);

                            // Reference Tables and print
                            viewChatlogTable(DbConnection);

                            // Close the connection.
                            DbConnection.close();
                        } catch (Exception e)
                        {
                            System.out.println("Error Creating the Chatlog Table");
                            System.out.println(e.getMessage());
                        }

                }
            } // end while(keepGoing), proceed to remove disconnect client
            remove(id); // remove clientThread UID from client list
            close(); // close clientThread streams & socket
        } // end clientThread

        /** close everything **/
        private void close() {
            // close the connection (inputStream, outputStream, & socket, )
            try {
                if(sOutputStream != null) sOutputStream.close();
            }
            catch(Exception e) {}
            try {
                if(sInputStream != null) sInputStream.close();
            }
            catch(Exception e) {}
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        /** method to send a msg string via client thread to respective output stream **/
        private boolean sendMsg(String msg) {
            // if client not connected call close method
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // if client connected write message via output stream
            try {
                sOutputStream.writeObject(msg + "\n");
            }
            // inform user of any errors but don't abort
            catch(IOException e) {
                display("Error sending message to " + username);
                display("\n" + e.toString());
            }
            return true;
        }
    } // end clientThread class


    /**
     * Creates a database for storing Chatlog information and Userlist
     */
    public void CreateChatlogDB()
    {
        try
        {
            // Create a named constant for the URL.
            // NOTE: This value is specific for Java DB.
            final String DB_URL = "jdbc:derby:ChatlogDB;create=true";

            // Create a connection to the database.
            Connection DbConnection = DriverManager.getConnection(DB_URL);

            // If the DB already exists, drop the tables.
            dropTables(DbConnection);

            // Build the Chatlog table.
            buildChatlogTable(DbConnection);

            // Build the Customer table.
            //buildUserlistTable(DbConnection);

            // Reference Tables and print
            //viewUserTable(DbConnection);

            // Close the connection.
            DbConnection.close();
        } catch (Exception e)
        {
            System.out.println("Error Creating the Chatlog Table");
            System.out.println(e.getMessage());
        }
    } // end create ChatlogDB()



    /** dropTables method drops any existing tables in case the database already exists. **/
    public void dropTables(Connection conn)
    {
        System.out.println("Checking for existing tables.");
        try
        {
            Statement stmt = conn.createStatement();    // Get a Statement object.
            /**
            try
            {
                stmt.execute("DROP TABLE Userlist");    // Drop the Userlist table.
                System.out.println("Customer table dropped.");
            } catch (SQLException ex)
            {
                // No need to report an error.
                // The table simply did not exist.
            }
             **/
            try
            {

                stmt.execute("DROP TABLE Chatlog");      // Drop the Chatlog table.
                System.out.println("Chatlog table dropped.");
            } catch (SQLException ex)
            {
                // No need to report an error.
                // The table simply did not exist.
            }
        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** buildChatlogTable method creates the Chatlog table **/
    public void buildChatlogTable(Connection DbConnection)
    {
        try
        {
            Statement statement = DbConnection.createStatement();    // Get a Statement object.
            // Create the table.
            statement.execute("CREATE TABLE Chatlog (" + "Message CHAR(80) " + ")");


            /* * THIS SECTION WAS USED TO TEST DATABASE IMPLEMENTATION * */

            // Insert row #1.
            //statement.execute("INSERT INTO Chatlog VALUES ( " +
            //        "'12:51:43 <User1023>: Message history number 1')");

            // Insert row #1.
            //statement.execute("INSERT INTO Chatlog VALUES ( " +
            //        "'12:52:12 <User5192>: Message history number 2')");

            // Insert row #2.
            //statement.execute("INSERT INTO Chatlog VALUES ( " +
            //        "'13:00:10 <User4581>: Message history number 3')");


            System.out.println("Chatlog table created.");
        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
        }
    }

    /** addToChatlogTable method adds A row to the Chatlog table. **/
    public void addToChatlogTable(Connection DbConnection, String msg)
    {
        try
        {
            Statement statement = DbConnection.createStatement();    // Get a Statement object.

            String messageFormat = "'" + msg + "'";

            // Insert row content
            statement.execute("INSERT INTO Chatlog VALUES ( " + "'" + msg + "')");
            //statement.execute("INSERT INTO Chatlog VALUES ( " + "'13:00:10 <User4581>: Message history number 3')");

            System.out.println("Chat message added to Chatlog table.");
        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
        }
    }

/**
 * * * * DECIDED THIS WAS UNNECESSARY TO IMPLEMENT * * * *
 *
     // buildUserlistTable method creates the Userlist table and adds some rows to it.
    public void buildUserlistTable(Connection DbConnection)
    {
        try
        {
            Statement statement = DbConnection.createStatement();    // Get a Statement object.
            // Create the table.
            statement.execute("CREATE TABLE Userlist" +
                    "( Username CHAR(10) )");

            // Add some rows to the new table.
            statement.executeUpdate("INSERT INTO Userlist VALUES" +
                    "('User1295')");

            statement.executeUpdate("INSERT INTO Userlist VALUES" +
                    "('User1043')");

            statement.executeUpdate("INSERT INTO Userlist VALUES" +
                    "('User9842')");

            System.out.println("Userlist table created.");
        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
        }
    }
 **/

    /**
     * The buildUnpaidOrderTable method creates
     * the UnpaidOrder table.
     */

/**
 * * * * DECIDED THIS WAS UNNECESSARY TO IMPLEMENT * * * *
 *
    public void viewUserTable(Connection DbConnection) {

        System.out.println("Checking for existing tables.");

        try
        {
            // Get a Statement object.
            Statement statement = DbConnection.createStatement();
            try
            {
                // Print the userlist table.
                ResultSet resultSet = statement.executeQuery(
                        "SELECT * FROM Userlist");
                clientCast("\n------------------------------------------------------------------------"
                        + "\nPrinting userlist history...\n");
                System.out.println("\nPrinting userlist table...");
                while (resultSet.next()) {
                    clientCast(resultSet.getString("Username"));
                    //System.out.println(
                    //        " " + resultSet.getString("Username") );
                }
                clientCast("\n------------------------------------------------------------------------");
                System.out.println("Done printing userlist table.");
            } catch (SQLException ex)
            {
                System.err.println("Exception: " + ex.getMessage());
                // No need to report an error.
                // The table simply did not exist.
            }

        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }

    }
 **/
    public void viewChatlogTable(Connection DbConnection) {

        System.out.println("Checking for existing tables.");

        try
        {
            // Get a Statement object.
            Statement statement = DbConnection.createStatement();

            try
            {
                // Print the chatlog table.
                ResultSet resultSet = statement.executeQuery(
                        "SELECT * FROM Chatlog");
                clientCast("\n------------------------------------------------------------------------"
                        + "\nPrinting chatlog history...\n");
                System.out.println("\nPrinting chatlog table...");
                while (resultSet.next()) {
                    clientCast(resultSet.getString("Message")
                    );
                    //System.out.println(" " + resultSet.getString("Message")
                    //);
                }
                clientCast("\n------------------------------------------------------------------------");
                System.out.println("Done printing chatlog table.");
            } catch (SQLException ex)
            {
                System.err.println("Exception: " + ex.getMessage());
                // No need to report an error.
                // The table simply did not exist.
            }

        } catch (SQLException ex)
        {
            System.out.println("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

} // end server class

