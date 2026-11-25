// Written by tib when the service was created; it is yours from then on.
package me.yakar.cornerservice;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * The service, started against PostgreSQL in a container on a free port, and a client of its API,
 * for the tests that call it over HTTP.
 */
public final class TestService implements AutoCloseable {

  private final HttpServer server;
  private final HttpClient client = HttpClient.newHttpClient();

  private TestService(HttpServer server) {
    this.server = server;
  }

  /** Starts the service against the database of the tests. */
  public static TestService start() throws IOException {
    return new TestService(CornerServiceApplication.start(TestDatabase.config(0)));
  }

  /** Sends a request without a body to the path, and gives the answer. */
  public HttpResponse<String> send(String method, String path)
      throws IOException, InterruptedException {
    return exchange(
        path, HttpRequest.newBuilder().method(method, HttpRequest.BodyPublishers.noBody()));
  }

  /** Sends a request to the path with the body as JSON, and gives the answer. */
  public HttpResponse<String> send(String method, String path, Object body)
      throws IOException, InterruptedException {
    return exchange(
        path,
        HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .method(
                method,
                HttpRequest.BodyPublishers.ofByteArray(Json.MAPPER.writeValueAsBytes(body))));
  }

  private HttpResponse<String> exchange(String path, HttpRequest.Builder request)
      throws IOException, InterruptedException {
    URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + path);
    return client.send(request.uri(uri).build(), HttpResponse.BodyHandlers.ofString());
  }

  /** The body of the answer, read as JSON into the type. */
  public static <T> T read(HttpResponse<String> answer, Class<T> type) {
    return Json.MAPPER.readValue(answer.body(), type);
  }

  @Override
  public void close() {
    client.close();
    server.stop(0);
  }
}
