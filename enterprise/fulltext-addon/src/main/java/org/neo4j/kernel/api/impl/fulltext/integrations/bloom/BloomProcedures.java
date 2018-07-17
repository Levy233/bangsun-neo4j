/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.fulltext.integrations.bloom;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.neo4j.kernel.api.impl.fulltext.FulltextProvider;
import org.neo4j.kernel.api.impl.fulltext.ReadOnlyFulltext;
import org.neo4j.kernel.api.impl.fulltext.ScoreEntityIterator;
import org.neo4j.kernel.api.impl.fulltext.ScoreEntityIterator.ScoreEntry;
import org.neo4j.internal.kernel.api.InternalIndexState;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import static org.neo4j.kernel.api.impl.fulltext.FulltextIndexType.NODES;
import static org.neo4j.kernel.api.impl.fulltext.FulltextIndexType.RELATIONSHIPS;
import static org.neo4j.kernel.api.impl.fulltext.integrations.bloom.BloomKernelExtensionFactory.BLOOM_NODES;
import static org.neo4j.kernel.api.impl.fulltext.integrations.bloom.BloomKernelExtensionFactory.BLOOM_RELATIONSHIPS;
import static org.neo4j.procedure.Mode.READ;
import static org.neo4j.procedure.Mode.SCHEMA;

/**
 * Procedures for querying the bloom fulltext addon.
 */
public class BloomProcedures
{
    @Context
    public FulltextProvider provider;

    private static final Function<ScoreEntry,EntityOutput> QUERY_RESULT_MAPPER = result -> new EntityOutput( result.entityId(), result.score() );

    @Description( "Await the completion of any background index population or updates" )
    @Procedure( name = "bloom.awaitPopulation", mode = READ )
    public void awaitPopulation() throws Exception
    {
        provider.awaitPopulation();
    }

    @Description( "Returns the node property keys indexed by the Bloom fulltext index add-on" )
    @Procedure( name = "bloom.getIndexedNodePropertyKeys", mode = READ )
    public Stream<PropertyOutput> getIndexedNodePropertyKeys() throws Exception
    {
        return provider.getProperties( BLOOM_NODES, NODES ).stream().map( PropertyOutput::new );
    }

    @Description( "Returns the relationship property keys indexed by the Bloom fulltext index add-on" )
    @Procedure( name = "bloom.getIndexedRelationshipPropertyKeys", mode = READ )
    public Stream<PropertyOutput> getIndexedRelationshipPropertyKeys() throws Exception
    {
        return provider.getProperties( BLOOM_NODES, NODES ).stream().map( PropertyOutput::new );
    }

    @Description( "Set the node property keys to index" )
    @Procedure( name = "bloom.setIndexedNodePropertyKeys", mode = SCHEMA )
    public void setIndexedNodePropertyKeys( @Name( "propertyKeys" ) List<String> propertyKeys ) throws Exception
    {
        provider.changeIndexedProperties( BLOOM_NODES, NODES, propertyKeys );
    }

    @Description( "Set the relationship property keys to index" )
    @Procedure( name = "bloom.setIndexedRelationshipPropertyKeys", mode = SCHEMA )
    public void setIndexedRelationshipPropertyKeys( @Name( "propertyKeys" ) List<String> propertyKeys ) throws Exception
    {
        provider.changeIndexedProperties( BLOOM_RELATIONSHIPS, RELATIONSHIPS, propertyKeys );
    }

    @Description( "Check the status of the Bloom fulltext index add-on" )
    @Procedure( name = "bloom.indexStatus", mode = READ )
    public Stream<StatusOutput> indexStatus() throws Exception
    {
        InternalIndexState nodeIndexState = provider.getState( BLOOM_NODES, NODES );
        InternalIndexState relationshipIndexState = provider.getState( BLOOM_RELATIONSHIPS, RELATIONSHIPS );
        return Stream.of( nodeIndexState, relationshipIndexState ).map( StatusOutput::new );
    }

    @Description( "Query the Bloom fulltext index for nodes" )
    @Procedure( name = "bloom.searchNodes", mode = READ )
    public Stream<EntityOutput> bloomFulltextNodes(
            @Name( "terms" ) List<String> terms,
            @Name( value = "fuzzy", defaultValue = "true" ) boolean fuzzy,
            @Name( value = "matchAll", defaultValue = "false" ) boolean matchAll ) throws Exception
    {
        try ( ReadOnlyFulltext indexReader = provider.getReader( BLOOM_NODES, NODES ) )
        {
            return queryAsStream( terms, indexReader, fuzzy, matchAll );
        }
    }

    @Description( "Query the Bloom fulltext index for relationships" )
    @Procedure( name = "bloom.searchRelationships", mode = READ )
    public Stream<EntityOutput> bloomFulltextRelationships(
            @Name( "terms" ) List<String> terms,
            @Name( value = "fuzzy", defaultValue = "true" ) boolean fuzzy,
            @Name( value = "matchAll", defaultValue = "false" ) boolean matchAll ) throws Exception
    {
        try ( ReadOnlyFulltext indexReader = provider.getReader( BLOOM_RELATIONSHIPS, RELATIONSHIPS ) )
        {
            return queryAsStream( terms, indexReader, fuzzy, matchAll );
        }
    }

    private Stream<EntityOutput> queryAsStream( List<String> terms, ReadOnlyFulltext indexReader, boolean fuzzy, boolean matchAll )
    {
        ScoreEntityIterator resultIterator;
        if ( fuzzy )
        {
            resultIterator = indexReader.fuzzyQuery( terms, matchAll );
        }
        else
        {
            resultIterator = indexReader.query( terms, matchAll );
        }
        return resultIterator.stream().map( QUERY_RESULT_MAPPER );
    }

    public static class EntityOutput
    {
        public final long entityid;
        public final double score;

        EntityOutput( long entityid, float score )
        {
            this.entityid = entityid;
            this.score = score;
        }
    }

    public static class PropertyOutput
    {
        public final String propertyKey;

        PropertyOutput( String propertykey )
        {
            this.propertyKey = propertykey;
        }
    }

    public class StatusOutput
    {
        public final String state;

        StatusOutput( InternalIndexState internalIndexState )
        {
            switch ( internalIndexState )
            {
            case POPULATING:
                state = "POPULATING";
                break;
            case ONLINE:
                state = "ONLINE";
                break;
            case FAILED:
                state = "FAILED";
                break;
            default:
                throw new IllegalArgumentException( String.format( "Illegal index state %s", internalIndexState ) );
            }
        }
    }
}
