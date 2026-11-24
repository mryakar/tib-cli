// Written by tib with the first entity of the service; it is yours from then on.
package com.example.vehicleservice;

import java.io.Serial;

/**
 * What a use case throws for an entity that is not there; {@link ErrorHandler} answers it with 404
 * Not Found, so that the services know no HTTP.
 */
public class NotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super(message);
  }
}
