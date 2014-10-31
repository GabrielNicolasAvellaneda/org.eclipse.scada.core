/*******************************************************************************
 * Copyright (c) 2010, 2013 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - refactor for generic memory devices
 *******************************************************************************/
package org.eclipse.scada.da.server.common.memory;

import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.eclipse.scada.core.Variant;

/**
 * Implement a single bit attribute
 *
 * @author Jens Reimann
 */
public class FloatAttribute extends AbstractAttribute
{
    private final int index;

    private Float lastValue;

    private Variant lastTimestamp;

    private final boolean enableTimestamp;

    public FloatAttribute ( final String name, final int index, final boolean enableTimestamp )
    {
        super ( name );
        this.index = index;
        this.enableTimestamp = enableTimestamp;
    }

    @Override
    public void handleData ( final IoBuffer data, final Map<String, Variant> attributes, final Variant timestamp )
    {
        final float f = data.getFloat ( toAddress ( this.index ) );
        attributes.put ( this.name, Variant.valueOf ( f ) );

        if ( !Float.valueOf ( f ).equals ( this.lastValue ) )
        {
            this.lastValue = f;
            this.lastTimestamp = timestamp;
        }

        if ( this.enableTimestamp )
        {
            attributes.put ( this.name + ".timestamp", this.lastTimestamp );
        }
    }

    @Override
    public void handleError ( final Map<String, Variant> attributes )
    {
        this.lastValue = null;
        this.lastTimestamp = null;
    }

    @Override
    public void handleWrite ( final Variant value )
    {
        final MemoryRequestBlock block = this.block;

        if ( block == null )
        {
            throw new IllegalStateException ( "Device is not connected" );
        }

        final Double d = value.asDouble ( null );
        if ( d != null )
        {
            final ByteBuffer b = ByteBuffer.allocate ( 4 );
            b.putFloat ( d.floatValue () );
            block.writeData ( toAddress ( this.index ), b.array () );
        }
    }

}
