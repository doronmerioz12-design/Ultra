package branches;

import model.Urgency;

public interface Observer {

    void update(String decryptedText, Urgency urgencyLevel);
}
