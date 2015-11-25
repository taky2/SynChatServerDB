import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/*********************************************************************************************************
 * Server-side GUI app runs a multithreaded server allowing multiple clients to connect and synchronize  *
 *                                                                                                       *
 *      Server also creates a Derby database to store and reference chat log history                     *
 *                                                                                                       *
 *      DEPENDENCIES:   Java 1.8 or higher                                                               *
 *                      derby.jar, derbyclient.jar, derbynet.jar, derbytools.jar                         *
 *********************************************************************************************************/

public class ServerUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 7145714966993813033L;

    private JButton stopStart;      // stop & start button
    private JTextArea chat, event;  // JTextArea for chat room
    private JTextField tPortNumber; // port number
    private Server server;          // server


    public static void main(String[] arg) {
        new ServerUI(8700);    // start server default port 8700
    }

    /**
     * server constructor
     *  @param port: port to listen
     **/
    ServerUI(int port) {
        super("Chat Server");
        server = null;
        // construct topPanel layout
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("  " + port);
        topPanel.add(tPortNumber); // add textField for port number input
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);  // add start button to display
        topPanel.add(stopStart);
        add(topPanel, BorderLayout.NORTH);

        // construct chat room layout
        JPanel centerPanel = new JPanel(new GridLayout(2,1));
        chat = new JTextArea(60,45);
        chat.setEditable(false);
        appendRoom("------------------------------------------------------------------------\n"
                + "                                                            Chat room\n"
                + "------------------------------------------------------------------------\n");
        centerPanel.add(new JScrollPane(chat));
        event = new JTextArea(60,45);
        event.setEditable(false);
        appendEvent("------------------------------------------------------------------------\n"
                + "                                                           Events log\n"
                + "------------------------------------------------------------------------\n");
        centerPanel.add(new JScrollPane(event));
        add(centerPanel);

        addWindowListener(this); // listen for user click on close button of frame
        setSize(600, 600);
        setVisible(true);
    }

    /** append message to the end of GUI's JTextAreas with following methods **/
    void appendRoom(String str) {
        chat.append(str);
        chat.setCaretPosition(chat.getText().length() - 1);
    }
    void appendEvent(String str) {
        event.append(str);
        event.setCaretPosition(chat.getText().length() - 1);
    }

    /** start/stop server on click **/
    public void actionPerformed(ActionEvent e) {
        // if running we have to stop to synchronize
        if(server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        // start server
        int port;
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
        }
        catch(Exception er) {
            appendEvent("Invalid port number");
            return;
        }
        server = new Server(port, this);    // create new server
        new ServerRunning().start();        // on a new thread
        stopStart.setText("Stop");          // update button text
        tPortNumber.setEditable(false);     // protect port number
    }


    /** Handle event: user closes window **/
    public void windowClosing(WindowEvent e) {
        // if server exists
        if(server != null) {
            try {
                server.stop();	// ask server to close connection
            }
            catch(Exception eClose) {
            }
            server = null;
        }
        dispose(); // frame
        System.exit(0);
    }
    // Ignore...
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    /** thread to run server **/
    class ServerRunning extends Thread {
        public void run() {
            server.start();
            // the server failed or was stopped
            stopStart.setText("Start");
            tPortNumber.setEditable(true);
            appendEvent("Warning: Server stopped. If no stop request was made server may have crashed.\n");
            server = null;
        }
    }
} // end serverGUI
