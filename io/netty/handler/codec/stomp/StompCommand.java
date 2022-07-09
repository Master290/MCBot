package io.netty.handler.codec.stomp;

public enum StompCommand
{
  STOMP,  CONNECT,  CONNECTED,  SEND,  SUBSCRIBE,  UNSUBSCRIBE,  ACK,  NACK,  BEGIN,  ABORT,  COMMIT,  DISCONNECT,  MESSAGE,  RECEIPT,  ERROR,  UNKNOWN;
  
  private StompCommand() {}
}
