package org.neo4j.server.enterprise;

import org.neo4j.server.BlockingBootstrapper;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.ServerBootstrapper;

public class CommercialEntryPoint {
    private static Bootstrapper bootstrapper;

    private CommercialEntryPoint() {
    }

    public static void main(String[] args) {
        int status = ServerBootstrapper.start(new CommercialBootstrapper(), args);
        if(status != 0) {
            System.exit(status);
        }

    }

    public static void start(String[] args) {
        bootstrapper = new BlockingBootstrapper(new CommercialBootstrapper());
        System.exit(ServerBootstrapper.start(bootstrapper, args));
    }

    public static void stop(String[] args) {
        if(bootstrapper != null) {
            bootstrapper.stop();
        }

    }
}
