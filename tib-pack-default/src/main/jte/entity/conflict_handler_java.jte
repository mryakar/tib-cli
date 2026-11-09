@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a request that breaks a constraint of the database, such as a value that has to be
 * unique or a row that others refer to, with 409 Conflict. The answer does not say which
 * constraint, so that the schema stays inside the service.
 */
@RestControllerAdvice
public class ConflictHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail conflict() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The request breaks a constraint of the database.");
  }
}
