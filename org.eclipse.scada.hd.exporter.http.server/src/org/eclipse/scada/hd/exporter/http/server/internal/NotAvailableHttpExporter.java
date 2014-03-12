/*******************************************************************************
 * Copyright (c) 2010, 2014 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - some bugfixes and modifications
 *******************************************************************************/
package org.eclipse.scada.hd.exporter.http.server.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.scada.hd.exporter.http.DataPoint;
import org.eclipse.scada.hd.exporter.http.HttpExporter;

/**
 * just a placeholder for the case that no exporter is available
 * @author jrose
 */
public class NotAvailableHttpExporter implements HttpExporter
{
    public List<DataPoint> getData ( final String item, final String type, final Date from, final Date to, final Integer number )
    {
        return new ArrayList<DataPoint> ();
    }

    public List<String> getItems ()
    {
        return new ArrayList<String> ();
    }

    public List<String> getSeries ( final String itemId )
    {
        return new ArrayList<String> ();
    }

    @Override
    public void dispose () throws Exception
    {
    }
}
