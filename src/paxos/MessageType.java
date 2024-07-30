package paxos;

public enum MessageType {
    PREPARE_REQUEST,
    PREPARE_ANSWER,
    ACCEPT_REQUEST,
    ACCEPT_ACK,
    LEARN
}
