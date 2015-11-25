import java.io.*;

/***********************************************************************************
 * Constructs a ChatMessage object                                                 *
 *                                                                                 *
 * defines type of message exchanged as individual object passed by client/sever   *
 ***********************************************************************************/

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 7145714966993813033L;

    /* Types of message objects:
    *
    * USERLIST to receive the list of the users connected
    * MESSAGE an ordinary message
    * LOGOUT to disconnect from the Server
    * HISTORY to view chatlog history
    */
    static final int USERLIST = 0, MESSAGE = 1, LOGOUT = 2, HISTORY = 3;
    private int type;
    private String message;

    // mutator constructor
    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // accessors
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }
}
