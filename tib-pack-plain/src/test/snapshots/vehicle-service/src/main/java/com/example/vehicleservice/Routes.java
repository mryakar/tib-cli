// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice;

import com.example.vehicleservice.depot.DepotWiring;
import com.example.vehicleservice.vehicle.VehicleWiring;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The API of the entities of the spec: for every entity with a service, the handler that its wiring
 * makes, at the path of its entities. {@link ErrorHandler} answers what a handler throws.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    route(server, "/api/depots", DepotWiring.handler(transactions));
    route(server, "/api/vehicles", VehicleWiring.handler(transactions));
  }

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }
}
