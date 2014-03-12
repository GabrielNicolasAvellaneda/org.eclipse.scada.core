/*******************************************************************************
 * Copyright (c) 2010, 2014 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - generic subscription manager
 *******************************************************************************/
package org.eclipse.scada.ae.server;

import java.util.List;
import java.util.Set;

import org.eclipse.scada.ae.data.MonitorStatusInformation;
import org.eclipse.scada.core.subscription.SubscriptionListener;

public interface MonitorListener extends SubscriptionListener<String>
{
    public void dataChanged ( String subscriptionId, List<MonitorStatusInformation> addedOrUpdated, Set<String> removed, boolean full );
}
