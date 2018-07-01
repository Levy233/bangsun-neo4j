package org.neo4j.kernel.cluster;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.LoadableConfig;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.configuration.Title;

import java.time.Duration;
import java.util.List;

import static org.neo4j.kernel.configuration.Settings.*;
import static org.neo4j.kernel.configuration.Settings.INTEGER;
import static org.neo4j.kernel.configuration.Settings.setting;

/**
 * Created by Think on 2018/6/28.
 */
@Description( "Bangsun Ha Cluster configuration settings" )
public class BsClusterSettings implements LoadableConfig{
    @Title( "Bangsun cluster database" )
    @Description( "Only allow read operations from this Neo4j instance. " +
            "This mode still requires write access to the directory for lock purposes." )
    public static final Setting<Boolean> bs_is_cluster = setting( "bs.is_cluster", BOOLEAN, FALSE );

    @Description( "Duration for which master will buffer ids and not reuse them to allow slaves read " +
            "consistently. Slaves will also terminate transactions longer than this duration, when " +
            "applying received transaction stream, to make sure they do not read potentially " +
            "inconsistent/reused records." )
    @Internal
    public static final Setting<Duration> id_reuse_safe_zone_time = setting( "unsupported.dbms.id_reuse_safe_zone",
            Settings.DURATION, "1h" );

//    @Title( "Master instance" )
//    @Description( "Only allow read operations from this Neo4j instance. " +
//            "This mode still requires write access to the directory for lock purposes." )
//    public static final Setting<Boolean> bs_is_master = setting( "bs.is_master", BOOLEAN, FALSE );

//    @Title( "Slave instance" )
//    @Description( "Only allow read operations from this Neo4j instance. " +
//            "This mode still requires write access to the directory for lock purposes." )
//    public static final Setting<Boolean> bs_is_slave = setting( "bs.is_slave", BOOLEAN, FALSE );

//    @Description( "A comma-separated list of other members of the cluster to join." )
//    public static final Setting<List<HostnamePort>> bs_slave_hosts = setting( "bs.slave_hosts",
//            list( ",", HOSTNAME_PORT ), NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<List<HostnamePort>> bs_cluster_hosts = setting( "bs.cluster_hosts",
            list( ",", HOSTNAME_PORT ), NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<HostnamePort> bs_my_host = setting( "bs.my_host",
            HOSTNAME_PORT, NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<Integer> bs_instance_id = setting( "bs.instance_id",
            INTEGER, NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<Integer> bs_lost_time_out = setting( "bs.lost_time_out",
            INTEGER, "3" );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<Integer> bs_connect_time_out = setting( "bs.connect_time_out",
            INTEGER, "20" );
}
