@import me.yakar.tib.generate.Project
@import me.yakar.tib.spec.Architecture
@param Project project
!{boolean notFound = project.architecture() != Architecture.ANEMIC;}
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
@if(notFound)
 * A request for an entity that is not there is answered with 404 Not Found.
@endif
 */
@RestControllerAdvice
public class ConflictHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail conflict() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The request breaks a constraint of the database.");
  }
  @if(notFound)

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail notFound(NotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }
  @endif
}
