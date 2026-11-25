@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

/**
 * What a handler throws for a request that it cannot take, such as a DTO that does not read or
 * that breaks a rule; {@link ErrorHandler} answers it with 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
