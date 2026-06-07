package decrypt;

import model.Message;

public interface DecryptionStrategy {
    String decrypt(Message message);
}
