package org.neo4j.server.enterprise;

import org.neo4j.server.CommunityBootstrapper;
import org.neo4j.server.ServerCommandLineArgs;

import java.io.IOException;
import java.util.Collections;

import static org.neo4j.commandline.Util.neo4jVersion;

/**
 * Created by Think on 2018/5/3.
 */
public class BootstrapTest {
    public static void main( String[] argv ) throws IOException
    {
        ServerCommandLineArgs args = ServerCommandLineArgs.parse( argv );
        if ( args.version() )
        {
            System.out.println( "neo4j " + neo4jVersion() );
        }
        else
        {
//            int status = new CommercialBootstrapper().start(args.homeDir(), args.configFile(), Collections.<String, String>emptyMap());
            int status = new CommunityBootstrapper().start(args.homeDir(), args.configFile(), Collections.<String, String>emptyMap());
            if ( status != 0 )
            {
                System.exit( status );
            }
        }
    }

//    public static void main( String[] argv ) throws IOException
//    {
//        ServerCommandLineArgs args = ServerCommandLineArgs.parse( argv );
//        if ( args.version() )
//        {
//            System.out.println( "neo4j " + neo4jVersion() );
//        }
//        else
//        {
//            int status = new CommunityBootstrapper().start(args.homeDir(), args.configFile(), Collections.<String, String>emptyMap());
//            if ( status != 0 )
//            {
//                System.exit( status );
//            }
//        }
//    }
}

