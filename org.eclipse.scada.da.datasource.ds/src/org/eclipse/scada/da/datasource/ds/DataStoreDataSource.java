/*******************************************************************************
 * Copyright (c) 2010, 2014 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     Jens Reimann - additional work
 *     IBH SYSTEMS GmbH - add timestamp handling, fix logger
 *******************************************************************************/
package org.eclipse.scada.da.datasource.ds;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Executor;

import org.eclipse.scada.ca.ConfigurationDataHelper;
import org.eclipse.scada.core.OperationException;
import org.eclipse.scada.core.Variant;
import org.eclipse.scada.core.data.SubscriptionState;
import org.eclipse.scada.core.server.OperationParameters;
import org.eclipse.scada.da.client.DataItemValue.Builder;
import org.eclipse.scada.da.core.WriteAttributeResult;
import org.eclipse.scada.da.core.WriteAttributeResults;
import org.eclipse.scada.da.core.WriteResult;
import org.eclipse.scada.da.datasource.base.AbstractDataSource;
import org.eclipse.scada.da.server.common.WriteAttributesHelper;
import org.eclipse.scada.ds.DataListener;
import org.eclipse.scada.ds.DataNode;
import org.eclipse.scada.ds.DataNodeTracker;
import org.eclipse.scada.utils.concurrent.InstantErrorFuture;
import org.eclipse.scada.utils.concurrent.InstantFuture;
import org.eclipse.scada.utils.concurrent.NotifyFuture;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreDataSource extends AbstractDataSource implements DataListener
{
    private final static Logger logger = LoggerFactory.getLogger ( DataStoreDataSource.class );

    private static final String ATTR_TIMESTAMP = "timestamp"; //$NON-NLS-1$

    private final Executor executor;

    private boolean disposed;

    private final DataNodeTracker dataNodeTracker;

    private final String id;

    private String nodeId;

    private final BundleContext context;

    private Value currentNodeValue;

    private static class Value implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private Variant value = Variant.NULL;

        private Long timestamp;

        public Value ()
        {
        }

        public Value ( final Value other )
        {
            this.value = other.value;
            this.timestamp = other.timestamp;
        }

        public Variant getValue ()
        {
            return this.value;
        }

        public void setValue ( final Variant value )
        {
            this.value = value;
        }

        public Long getTimestamp ()
        {
            return this.timestamp;
        }

        public void setTimestamp ( final Long timestamp )
        {
            this.timestamp = timestamp;
        }

    }

    public DataStoreDataSource ( final BundleContext context, final String id, final Executor executor, final DataNodeTracker dataNodeTracker )
    {
        this.context = context;
        this.id = id;
        this.executor = executor;
        this.dataNodeTracker = dataNodeTracker;

        setError ( null );
    }

    @Override
    protected Executor getExecutor ()
    {
        return this.executor;
    }

    @Override
    public NotifyFuture<WriteAttributeResults> startWriteAttributes ( final Map<String, Variant> attributes, final OperationParameters operationParameters )
    {
        // copy first, whoever wins, it must be consistent
        final Value newValue = new Value ( this.currentNodeValue );

        final WriteAttributeResults initialResults = new WriteAttributeResults ();
        handleTimestamp ( initialResults, attributes, newValue );

        if ( setNewValue ( newValue ) )
        {
            return new InstantFuture<WriteAttributeResults> ( WriteAttributesHelper.errorUnhandled ( initialResults, attributes ) );
        }
        else
        {
            return new InstantErrorFuture<WriteAttributeResults> ( new OperationException ( "Unable to write to data store! Data store missing!" ) );
        }
    }

    private void handleTimestamp ( final WriteAttributeResults initialResults, final Map<String, Variant> attributes, final Value newValue )
    {
        final Variant timestampValue = attributes.get ( ATTR_TIMESTAMP );
        if ( timestampValue != null )
        {
            newValue.setTimestamp ( timestampValue.asLong ( null ) );
            initialResults.put ( ATTR_TIMESTAMP, WriteAttributeResult.OK );
        }
    }

    @Override
    public NotifyFuture<WriteResult> startWriteValue ( final Variant value, final OperationParameters operationParameters )
    {
        // copy first, whoever wins, it must be consistent
        final Value newValue = new Value ( this.currentNodeValue );
        newValue.setValue ( value );

        // now apply
        if ( setNewValue ( newValue ) )
        {
            return new InstantFuture<WriteResult> ( WriteResult.OK );
        }
        else
        {
            return new InstantErrorFuture<WriteResult> ( new OperationException ( "Unable to write to data store! Data store missing!" ) );
        }
    }

    private boolean setNewValue ( final Value newValue )
    {
        return this.dataNodeTracker.write ( new DataNode ( getNodeId (), newValue ) );
    }

    private String getNodeId ()
    {
        return this.nodeId;
    }

    public synchronized void update ( final Map<String, String> parameters ) throws Exception
    {
        if ( this.disposed )
        {
            return;
        }

        if ( this.nodeId != null )
        {
            this.dataNodeTracker.removeListener ( this.nodeId, this );
        }

        final ConfigurationDataHelper cfg = new ConfigurationDataHelper ( parameters );
        this.nodeId = cfg.getString ( "node.id", "org.eclipse.scada.da.datasource.ds/" + this.id ); //$NON-NLS-1$ //$NON-NLS-2$

        this.dataNodeTracker.addListener ( this.nodeId, this );
    }

    public synchronized void dispose ()
    {
        this.disposed = true;
        if ( this.nodeId != null )
        {
            this.dataNodeTracker.removeListener ( this.nodeId, this );
            this.nodeId = null;
        }
    }

    protected Value convertValue ( final DataNode node ) throws IOException, ClassNotFoundException
    {
        if ( node == null )
        {
            return new Value ();
        }

        final Object value = node.getDataAsObject ( this.context.getBundle () );
        if ( value instanceof Value )
        {
            return (Value)value;
        }
        else if ( value instanceof Variant )
        {
            final Value result = new Value ();
            result.setValue ( (Variant)value );
            return result;
        }
        return new Value ();
    }

    @Override
    public void nodeChanged ( final DataNode node )
    {
        logger.debug ( "Node data changed: {}", node ); //$NON-NLS-1$

        try
        {
            this.currentNodeValue = convertValue ( node );

            final Builder builder = new Builder ();
            builder.setSubscriptionState ( SubscriptionState.CONNECTED );
            builder.setValue ( this.currentNodeValue.getValue () );
            if ( this.currentNodeValue.getTimestamp () != null )
            {
                builder.setTimestamp ( this.currentNodeValue.getTimestamp () );
            }
            updateData ( builder.build () );
        }
        catch ( final Throwable e )
        {
            setError ( e );
        }
    }

    private void setError ( final Throwable e )
    {
        if ( e != null )
        {
            logger.warn ( "Failed to read data", e ); //$NON-NLS-1$
        }

        final Builder builder = new Builder ();
        builder.setSubscriptionState ( SubscriptionState.CONNECTED );
        builder.setValue ( Variant.NULL );
        builder.setAttribute ( "node.error", Variant.TRUE ); //$NON-NLS-1$

        if ( e != null )
        {
            builder.setAttribute ( "node.error.message", Variant.valueOf ( e.getMessage () ) ); //$NON-NLS-1$
        }

        updateData ( builder.build () );
    }
}
