@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Answers what the handlers throw, as problem details: an entity that is not there with 404 Not
 * Found; a request that breaks a constraint of the database, such as a value that has to be unique
 * or a row that others refer to, with 409 Conflict, without saying which constraint, so that the
 * schema stays inside the service; a request that the handler cannot take with 400 Bad Request; and
 * anything else with 500 Internal Server Error, logged.
 */
public class ErrorHandler extends Filter {

  private static final System.Logger LOG = System.getLogger(ErrorHandler.class.getName());

  /** The class of the SQL states of a broken constraint, as in 23505, a unique violation. */
  private static final String CONSTRAINT = "23";

  @Override
  public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
    try {
      chain.doFilter(exchange);
    } catch (NotFoundException e) {
      Http.problem(exchange, 404, e.getMessage());
    } catch (BadRequestException e) {
      Http.problem(exchange, 400, e.getMessage());
    } catch (Transactions.DatabaseException e) {
      String state = e.getCause().getSQLState();
      if (state != null && state.startsWith(CONSTRAINT)) {
        Http.problem(exchange, 409, "The request breaks a constraint of the database.");
      } else {
        failed(exchange, e);
      }
    } catch (RuntimeException e) {
      failed(exchange, e);
    }
  }

  private static void failed(HttpExchange exchange, RuntimeException e) throws IOException {
    LOG.log(System.Logger.Level.ERROR, exchange.getRequestMethod() + " " + exchange.getRequestURI() + " failed", e);
    Http.problem(exchange, 500, "The service failed.");
  }

  @Override
  public String description() {
    return "Answers the errors of the handlers as problem details";
  }
}
