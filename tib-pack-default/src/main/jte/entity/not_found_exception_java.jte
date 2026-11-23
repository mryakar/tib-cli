@import me.yakar.tib.generate.Project
@param Project project
package ${project.basePackage()};

/**
 * What a use case throws for an entity that is not there; {@link ConflictHandler} answers it with
 * 404 Not Found, so that the services know no HTTP.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
