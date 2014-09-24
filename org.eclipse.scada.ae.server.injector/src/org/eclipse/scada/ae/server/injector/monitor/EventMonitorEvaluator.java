/*******************************************************************************
 * Copyright (c) 2010, 2014 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - generalize event injection
 *******************************************************************************/
package org.eclipse.scada.ae.server.injector.monitor;

import org.eclipse.scada.ae.Event;

public interface EventMonitorEvaluator
{
    public Event evaluate ( Event event );
}
