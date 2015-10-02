package Helpers;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class MessageHolder implements Serializable {

    public String message;

    /* Join Parameters */
    public String joiningNode;
    public LinkedList list;

    /* Insert/Query/Delete Parameters */
    public String key;
    public String value;
    public String portNo;
    public boolean responseForAll;
    public HashMap<String, String> resultMap;

    /* Join Constructors */
    public MessageHolder(String msg) {
        this.message = msg;
    }

    public MessageHolder(String msg, String joinNode) {
        this.message = msg;
        this.joiningNode = joinNode;
    }

    public MessageHolder(String msg, LinkedList l) {
        this.message = msg;
        this.list = l;
    }

    /* Insert/Query/Delete Constructors */
    public MessageHolder(String msg, String key, String val) {
        this.message = msg;
        this.key = key;
        this.value = val;
    }

    public MessageHolder(String msg, String key, String val, String portNumber) {
        this.message = msg;
        this.key = key;
        this.value = val;
        this.portNo = portNumber;
    }

    public MessageHolder(String msg, HashMap<String, String> map, boolean respAll) {
        this.message = msg;
        this.resultMap = map;
        this.responseForAll = respAll;
    }
}
