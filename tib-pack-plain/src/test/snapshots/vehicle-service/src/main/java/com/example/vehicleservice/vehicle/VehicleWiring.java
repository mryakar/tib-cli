// Written by tib when the entity was created; it is yours from then on.
package com.example.vehicleservice.vehicle;

import com.example.vehicleservice.Transactions;
import com.sun.net.httpserver.HttpHandler;

/**
 * The classes of vehicles, wired by hand. {@code Routes} takes the handler of their API from here;
 * what a class of theirs takes in its constructor, it is given here.
 */
public final class VehicleWiring {

  private VehicleWiring() {}

  /** The handler of the API of vehicles, on their service, which keeps them in the repository. */
  public static HttpHandler handler(Transactions transactions) {
    VehicleRepository repository = new VehicleRepository(transactions);
    VehicleService service = new VehicleService(transactions, repository, new VehicleMapper());
    return new VehicleHandler(service);
  }
}
