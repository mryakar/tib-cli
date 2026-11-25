// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON of the API: one mapper, configured once, that reads the requests and writes the answers.
 * Dates and times are ISO-8601 text, and a property that a DTO does not have is ignored, as Spring
 * Boot does.
 */
public final class Json {

  public static final JsonMapper MAPPER =
      JsonMapper.builder()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  private Json() {}
}
