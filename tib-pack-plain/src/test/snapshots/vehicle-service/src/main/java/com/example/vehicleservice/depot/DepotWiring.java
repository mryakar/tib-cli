// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.depot;

import com.example.vehicleservice.Transactions;
import com.sun.net.httpserver.HttpHandler;

/**
 * The classes of depots, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class DepotWiring {

  private DepotWiring() {}

  /** The handler of the API of depots, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    DepotRepository repository = new DepotRepository(transactions);
    DepotService service = new DepotService(transactions, repository, new DepotMapper());
    return new DepotHandler(service);
  }
}
