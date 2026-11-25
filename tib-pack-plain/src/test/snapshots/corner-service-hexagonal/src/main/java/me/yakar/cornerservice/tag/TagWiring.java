// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.tag;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.tag.adapter.in.web.TagHandler;
import me.yakar.cornerservice.tag.adapter.in.web.TagWebMapper;
import me.yakar.cornerservice.tag.adapter.out.persistence.TagPersistenceAdapter;
import me.yakar.cornerservice.tag.adapter.out.persistence.TagRepository;
import me.yakar.cornerservice.tag.application.TagService;
import me.yakar.cornerservice.tag.application.port.out.TagStore;

/**
 * The classes of tags, wired by hand. {@code Routes} takes the handler of their API from here; what
 * a class of theirs takes in its constructor, it is given here.
 */
public final class TagWiring {

  private TagWiring() {}

  /** The handler of the API of tags, on their use cases, which keep them in the store. */
  public static HttpHandler handler(Transactions transactions) {
    TagRepository repository = new TagRepository(transactions);
    TagStore store = new TagPersistenceAdapter(transactions, repository);
    TagService service = new TagService(transactions, store);
    return new TagHandler(service, new TagWebMapper());
  }
}
