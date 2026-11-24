// Written by tib with the first entity of the service; it is yours from then on.
package me.yakar.cornerservice;

import java.io.Serial;

/**
 * What a handler throws for a request that it cannot take, such as a DTO that does not read or that
 * breaks a rule; {@link ErrorHandler} answers it with 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BadRequestException(String message) {
    super(message);
  }
}
