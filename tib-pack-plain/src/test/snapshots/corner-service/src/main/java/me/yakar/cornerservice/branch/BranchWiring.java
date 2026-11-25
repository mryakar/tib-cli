// Written by tib when the entity was created; it is yours from then on.
package me.yakar.cornerservice.branch;

import com.sun.net.httpserver.HttpHandler;
import me.yakar.cornerservice.Transactions;

/**
 * The classes of branches, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class BranchWiring {

  private BranchWiring() {}

  /** The handler of the API of branches, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    BranchRepository repository = new BranchRepository(transactions);
    BranchService service = new BranchService(transactions, repository, new BranchMapper());
    return new BranchHandler(service);
  }
}
