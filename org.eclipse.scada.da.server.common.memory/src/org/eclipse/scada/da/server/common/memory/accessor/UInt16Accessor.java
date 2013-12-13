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
package org.eclipse.scada.da.server.common.memory.accessor;

import org.apache.mina.core.buffer.IoBuffer;

public class UInt16Accessor implements Getter<Integer>, Setter<Integer>
{
    public static final UInt16Accessor INSTANCE = new UInt16Accessor ();

    @Override
    public Integer get ( final IoBuffer data, final int index )
    {
        return data.getUnsignedShort ( index );
    }

    @Override
    public void put ( final IoBuffer data, final Integer value )
    {
        data.putUnsignedShort ( value );
    }
}