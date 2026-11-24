// Written by tib on every run. Do not edit: your changes would be lost.
package com.example.vehicleservice;

import com.example.vehicleservice.depot.DepotHandler;
import com.example.vehicleservice.depot.DepotMapper;
import com.example.vehicleservice.depot.DepotRepository;
import com.example.vehicleservice.depot.DepotService;
import com.example.vehicleservice.vehicle.VehicleHandler;
import com.example.vehicleservice.vehicle.VehicleMapper;
import com.example.vehicleservice.vehicle.VehicleRepository;
import com.example.vehicleservice.vehicle.VehicleService;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The API of the entities of the spec, wired by hand: for every entity with a service, its
 * repository, mapper, service and handler, the handler at the path of its entities, behind {@link
 * ErrorHandler}.
 */
public final class Routes {

  private Routes() {}

  /** Adds the API of every entity to the server, its work in the transactions of the database. */
  public static void add(HttpServer server, Transactions transactions) {
    route(
        server,
        "/api/depots",
        new DepotHandler(
            new DepotService(transactions, new DepotRepository(transactions), new DepotMapper())));
    route(
        server,
        "/api/vehicles",
        new VehicleHandler(
            new VehicleService(
                transactions, new VehicleRepository(transactions), new VehicleMapper())));
  }

  private static void route(HttpServer server, String path, HttpHandler handler) {
    server.createContext(path, handler).getFilters().add(new ErrorHandler());
  }
}
