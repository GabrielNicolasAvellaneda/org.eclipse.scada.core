/*******************************************************************************
 * Copyright (c) 2013 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.scada.hd.server.storage.slave.hds;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.scada.hd.data.HistoricalItemInformation;
import org.eclipse.scada.hd.server.storage.hds.AbstractStorageManager;
import org.eclipse.scada.hd.server.storage.hds.StorageInformation;
import org.eclipse.scada.hds.DataFilePool;
import org.eclipse.scada.utils.str.Tables;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageManager extends AbstractStorageManager
{

    private final static Logger logger = LoggerFactory.getLogger ( StorageManager.class );

    private final ScheduledExecutorService executor;

    private final ScheduledFuture<?> checkBaseJob;

    private BaseWatcher baseWatcher;

    private final Lock lock = new ReentrantLock ();

    private final BundleContext context;

    private final Map<File, StorageImpl> storages = new HashMap<File, StorageImpl> ();

    private final DataFilePool pool;

    private final ScheduledExecutorService eventExecutor;

    public StorageManager ( final BundleContext context, final File base, final DataFilePool pool, final ScheduledExecutorService executor, final ScheduledExecutorService eventExecutor )
    {
        super ( base );
        this.context = context;
        this.pool = pool;
        this.executor = executor;
        this.eventExecutor = eventExecutor;

        this.checkBaseJob = this.executor.scheduleWithFixedDelay ( new Runnable () {

            @Override
            public void run ()
            {
                checkBase ();
            }
        }, 0, Integer.getInteger ( "org.eclipse.scada.hd.server.storage.slave.hds.checkBaseSeconds", 60 ), TimeUnit.SECONDS );
    }

    protected void checkBase ()
    {
        logger.debug ( "Checking base {}", this.base );

        if ( this.base.isDirectory () && this.base.canRead () )
        {
            if ( this.baseWatcher == null )
            {
                logger.info ( "Base was found ... creating BaseWatcher" );
                try
                {
                    this.baseWatcher = new BaseWatcher ( this, this.base );
                }
                catch ( final IOException e )
                {
                    logger.warn ( "Failed to create base watcher", e );
                    this.baseWatcher = null;
                }
            }
        }
        else
        {
            if ( this.baseWatcher != null )
            {
                logger.info ( "Base is gone ... disposing" );
                this.baseWatcher.dispose ();
            }
        }
    }

    @Override
    public void dispose ()
    {
        logger.info ( "Disposing" );
        this.checkBaseJob.cancel ( false );
        super.dispose ();
    }

    @Override
    public String probe ( final File file )
    {
        return super.probe ( file );
    }

    void listfiles ( final PrintStream ps )
    {
        final List<List<String>> data = new LinkedList<> ();

        this.lock.lock ();
        try
        {
            for ( final Map.Entry<File, StorageImpl> entry : this.storages.entrySet () )
            {
                final HistoricalItemInformation hi = entry.getValue ().getInformation ();
                final StorageInformation si = entry.getValue ().getStorageInformation ();

                final LinkedList<String> row = new LinkedList<> ();
                data.add ( row );
                row.add ( "" + hi.getItemId () );
                row.add ( "" + si.getConfiguration ().getCount () );
                row.add ( "" + si.getConfiguration ().getTimeSlice () );
                row.add ( "" + entry.getKey () );
            }
        }
        finally
        {
            this.lock.unlock ();
        }

        Tables.showTable ( ps, Arrays.asList ( "ID", "File Count", "Time Slice", "Store" ), data, 2 );
    }

    public void addStorage ( final File storageDirectory ) throws Exception
    {
        logger.debug ( "Adding storage: {}", storageDirectory );

        this.lock.lock ();
        try
        {
            final StorageImpl storage = new StorageImpl ( this.context, storageDirectory, this.pool, this.queryExecutor, this.eventExecutor );
            this.storages.put ( storageDirectory, storage );
        }
        finally
        {
            this.lock.unlock ();
        }

    }

    public void removeStorage ( final File storageDirectory )
    {
        logger.debug ( "Removing storage: {}", storageDirectory );

        this.lock.lock ();
        try
        {
            final StorageImpl storage = this.storages.remove ( storageDirectory );
            if ( storage != null )
            {
                storage.dispose ();
            }
        }
        finally
        {
            this.lock.unlock ();
        }
    }

    public void fileChanged ( final File storageDirectory, final String id, final File fileChanged )
    {
        this.lock.lock ();
        logger.debug ( "fileChanged - storageDirectory: {}, id: {}, fileChanged: {}", storageDirectory, id, fileChanged );
        try
        {
            final StorageImpl storage = this.storages.get ( storageDirectory );
            if ( storage == null )
            {
                logger.info ( "Received change notification for unknown storage: {} / {}", storageDirectory, fileChanged );
                return;
            }
            storage.fileChanged ( fileChanged );
        }
        finally
        {
            this.lock.unlock ();
        }
    }
}
