public class Message {
    String id;
    String message;
    ReceiveThread th;
        
    public Message(String id, String message, ReceiveThread th) {
        this.id = id;
        this.message = message;
        this.th = th;
    }
}
