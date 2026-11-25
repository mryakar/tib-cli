// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON of the API: one mapper, which reads the requests and writes the answers. Its
 * configuration goes here.
 */
public final class Json {

  public static final JsonMapper MAPPER = JsonMapper.builder().build();

  private Json() {}
}
