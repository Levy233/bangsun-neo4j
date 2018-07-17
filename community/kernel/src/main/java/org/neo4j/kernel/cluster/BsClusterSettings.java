package org.neo4j.kernel.cluster;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.LoadableConfig;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.configuration.Title;
import org.neo4j.kernel.network.InstanceId;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.neo4j.kernel.configuration.Settings.*;
import static org.neo4j.kernel.configuration.Settings.INTEGER;
import static org.neo4j.kernel.configuration.Settings.setting;

/**
 * Created by Think on 2018/6/28.
 */
@Description( "Bangsun Ha Cluster configuration settings" )
public class BsClusterSettings implements LoadableConfig{
    public static final Function<String, InstanceId> INSTANCE_ID = new Function<String, InstanceId>()
    {
        @Override
        public InstanceId apply( String value )
        {
            try
            {
                return new InstanceId( Integer.parseInt( value ) );
            }
            catch ( NumberFormatException e )
            {
                throw new IllegalArgumentException( "not a valid integer value" );
            }
        }

        @Override
        public String toString()
        {
            return "an instance id, which has to be a valid integer";
        }
    };
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
    public static final Setting<List<HostnamePort>> bs_cluster_data_hosts = setting( "bs.cluster_data_hosts",
            list( ",", HOSTNAME_PORT ), NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<HostnamePort> bs_my_data_host = setting( "bs.my_data_host",
            HOSTNAME_PORT, NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<InstanceId> bs_instance_id = setting( "bs.instance_id",
            INSTANCE_ID, NO_DEFAULT );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<Integer> bs_lost_time_out = setting( "bs.lost_time_out",
            INTEGER, "3" );

    @Description( "A comma-separated list of other members of the cluster to join." )
    public static final Setting<Integer> bs_connect_time_out = setting( "bs.connect_time_out",
            INTEGER, "20" );

    @Description( "Max size of the data chunks that flows between master and slaves in HA. Bigger size may increase " +
            "throughput, but may also be more sensitive to variations in bandwidth, whereas lower size increases " +
            "tolerance for bandwidth variations." )
    public static final Setting<Long> com_chunk_size = buildSetting( "ha.data_chunk_size", BYTES, "2M" ).constraint( min( 1024L ) ).build();

    @Description( "Size of batches of transactions applied on slaves when pulling from master" )
    public static final Setting<Integer> pull_apply_batch_size = setting( "ha.pull_batch_size", INTEGER, "100" );

    @Description( "How long a slave will wait for response from master before giving up." )
    public static final Setting<Duration> read_timeout = setting( "ha.slave_read_timeout", DURATION, "20s" );

    @Description( "Timeout for taking remote (write) locks on slaves. Defaults to ha.slave_read_timeout." )
    public static final Setting<Duration> lock_read_timeout = buildSetting( "ha.slave_lock_timeout", DURATION ).inherits( read_timeout ).build();

    @Description( "Maximum number of connections a slave can have to the master." )
    public static final Setting<Integer> max_concurrent_channels_per_slave =
            buildSetting( "ha.max_channels_per_slave", INTEGER, "20" ).constraint( min( 1 ) ).build();


}
