package org.clementine_player.clementine.providers;

public class ProviderException extends Exception {
  private static final long serialVersionUID = 9058121055082615755L;
  
  public ProviderException(String message) {
    super(message);
  }
  
  public ProviderException(String message, Exception wrapped) {
    super(message, wrapped);
  }

  public ProviderException(Exception wrapped) {
    super(wrapped);
  }
}
