package org.neo4j.kernel.network.exception;

import org.neo4j.logging.Log;

public class ComException extends RuntimeException
{
    public static final boolean TRACE_HA_CONNECTIVITY = Boolean.getBoolean( "org.neo4j.com.TRACE_HA_CONNECTIVITY" );

    public ComException()
    {
        super();
    }

    public ComException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ComException( String message )
    {
        super( message );
    }

    public ComException( Throwable cause )
    {
        super( cause );
    }

    public ComException traceComException(Log log, String tracePoint )
    {
        if ( TRACE_HA_CONNECTIVITY )
        {
            String msg = String.format( "ComException@%x trace from %s: %s",
                    System.identityHashCode( this ), tracePoint, getMessage() );
            log.debug( msg, this, true );
        }
        return this;
    }
}