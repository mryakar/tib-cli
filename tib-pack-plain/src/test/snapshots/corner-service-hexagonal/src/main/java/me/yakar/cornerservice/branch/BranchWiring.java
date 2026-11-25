// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;
import me.yakar.cornerservice.branch.adapter.in.web.BranchHandler;
import me.yakar.cornerservice.branch.adapter.in.web.BranchWebMapper;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchPersistenceAdapter;
import me.yakar.cornerservice.branch.adapter.out.persistence.BranchRepository;
import me.yakar.cornerservice.branch.application.BranchService;
import me.yakar.cornerservice.branch.application.port.out.BranchStore;

/**
 * The classes of branches, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class BranchWiring {

  private BranchWiring() {}

  /** The handler of the API of branches, on their use cases, which keep them in the store. */
  public static HttpHandler handler(Transactions transactions) {
    BranchRepository repository = new BranchRepository(transactions);
    BranchStore store = new BranchPersistenceAdapter(transactions, repository);
    BranchService service = new BranchService(transactions, store);
    return new BranchHandler(service, new BranchWebMapper());
  }
}
