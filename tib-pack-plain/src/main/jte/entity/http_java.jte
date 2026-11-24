@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import tools.jackson.core.JacksonException;

/** What the handlers share: the parts of a request that they read, and the answers that they send. */
public final class Http {

  /** The size of a page when the request gives none, as in Spring Data. */
  static final int DEFAULT_SIZE = 20;

  /** The largest size of a page that a request may ask for, as in Spring Data. */
  static final int MAX_SIZE = 2000;

  private Http() {}

  /**
   * The identifier in the path of the request, after the path of its context, as 7 in
   * /api/vehicles/7; none for the path of the context itself.
   *
   * @throws NotFoundException for any other path under the context
   */
  public static Optional<String> id(HttpExchange exchange) {
    String path = exchange.getRequestURI().getPath();
    String rest = path.substring(exchange.getHttpContext().getPath().length());
    if (rest.isEmpty()) {
      return Optional.empty();
    }
    if (rest.length() > 1 && rest.lastIndexOf('/') == 0) {
      return Optional.of(rest.substring(1));
    }
    throw new NotFoundException("There is nothing at " + path);
  }

  /**
   * The identifier, read by the function.
   *
   * @throws BadRequestException when the function cannot read it
   */
  public static <T> T parse(String id, Function<String, T> read) {
    try {
      return read.apply(id);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("'" + id + "' is not an identifier");
    }
  }

  /** The number of the page that the request asks for, from 0: its parameter page, or 0. */
  public static int pageNumber(HttpExchange exchange) {
    return Math.max(0, parameter(exchange, "page", 0));
  }

  /**
   * The size of the page that the request asks for: its parameter size, or 20; at least 1 and at
   * most 2000.
   */
  public static int pageSize(HttpExchange exchange) {
    int size = parameter(exchange, "size", DEFAULT_SIZE);
    return size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
  }

  /**
   * A parameter of the query of the request that is a number, or the default when it is not there.
   *
   * @throws BadRequestException when it is not a number
   */
  static int parameter(HttpExchange exchange, String name, int defaultValue) {
    String query = exchange.getRequestURI().getRawQuery();
    if (query == null) {
      return defaultValue;
    }
    for (String pair : query.split("&")) {
      int equals = pair.indexOf('=');
      String key = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
      if (key.equals(name)) {
        String value = equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
        try {
          return Integer.parseInt(value);
        } catch (NumberFormatException e) {
          throw new BadRequestException(name + ": '" + value + "' is not a number");
        }
      }
    }
    return defaultValue;
  }

  /**
   * The body of the request, read as JSON into the type.
   *
   * @throws BadRequestException when it does not read
   */
  public static <T> T read(HttpExchange exchange, Class<T> type) {
    T value;
    try {
      value = Json.MAPPER.readValue(exchange.getRequestBody(), type);
    } catch (JacksonException e) {
      throw new BadRequestException("The body of the request does not read: " + e.getOriginalMessage());
    }
    if (value == null) {
      throw new BadRequestException("The body of the request is null");
    }
    return value;
  }

  /**
   * Checks what a request takes.
   *
   * @param problems the rules that it breaks, each as the value and the rule
   * @throws BadRequestException when it breaks one
   */
  public static void check(List<String> problems) {
    if (!problems.isEmpty()) {
      throw new BadRequestException(String.join("; ", problems));
    }
  }

  /** Answers with the status and the body as JSON. */
  public static void send(HttpExchange exchange, int status, Object body) throws IOException {
    send(exchange, status, "application/json", Json.MAPPER.writeValueAsBytes(body));
  }

  /** Answers with 204 No Content. */
  public static void noContent(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(204, -1);
    exchange.close();
  }

  /** Answers with 405 Method Not Allowed, and the methods that the path allows. */
  public static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
    exchange.getResponseHeaders().set("Allow", allowed);
    problem(exchange, 405, exchange.getRequestMethod() + " is not allowed here");
  }

  /** Answers with the status and a problem detail, as RFC 9457 has it. */
  public static void problem(HttpExchange exchange, int status, String detail) throws IOException {
    send(exchange, status, "application/problem+json", Json.MAPPER.writeValueAsBytes(new Problem(status, detail)));
  }

  private static void send(HttpExchange exchange, int status, String type, byte[] body)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
  }

  /** A problem detail: the status of the answer, and what the problem is. */
  record Problem(int status, String detail) {}
}
