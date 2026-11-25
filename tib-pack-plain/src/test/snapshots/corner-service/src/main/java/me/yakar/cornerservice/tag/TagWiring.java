// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;

/**
 * The classes of tags, wired by hand. {@code Routes} takes the handler of their API from here; what
 * a class of theirs takes in its constructor, it is given here.
 */
public final class TagWiring {

  private TagWiring() {}

  /** The handler of the API of tags, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    TagRepository repository = new TagRepository(transactions);
    TagService service = new TagService(transactions, repository, new TagMapper());
    return new TagHandler(service);
  }
}
