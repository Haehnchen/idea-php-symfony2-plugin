package fr.adrienbrault.idea.symfony2plugin.remote;


public interface MessageNotifier extends Runnable {
    void addMessageHandler(MessageHandler handler);
}
