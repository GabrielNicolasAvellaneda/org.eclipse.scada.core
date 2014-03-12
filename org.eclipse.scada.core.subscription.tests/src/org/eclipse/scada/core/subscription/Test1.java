/*******************************************************************************
 * Copyright (c) 2012, 2014 TH4 SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TH4 SYSTEMS GmbH - initial API and implementation
 *     IBH SYSTEMS GmbH - generic subscription manager
 *******************************************************************************/
package org.eclipse.scada.core.subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scada.core.data.SubscriptionState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Test1
{
    private SubscriptionManager<String> manager = null;

    @Before
    public void setup ()
    {
        this.manager = new SubscriptionManager<String> ();
    }

    @After
    public void cleanup ()
    {
        this.manager = null;
    }

    /**
     * Perform a simple subscribe/unsubscribe without a subscription source
     * attached
     * 
     * @throws Exception
     */
    @Test
    public void test1 () throws Exception
    {
        final SubscriptionRecorder<String> recorder = new SubscriptionRecorder<String> ();

        this.manager.subscribe ( "", recorder );
        this.manager.unsubscribe ( "", recorder );

        Assert.assertArrayEquals ( "Events are not the same", new SubscriptionStateEvent[] { new SubscriptionStateEvent ( SubscriptionState.GRANTED ), new SubscriptionStateEvent ( SubscriptionState.DISCONNECTED ) }, recorder.getList ().toArray ( new SubscriptionStateEvent[0] ) );

        Assert.assertEquals ( "Number of subscriptions does not match", 0, this.manager.getSubscriptionCount () );
    }

    /**
     * Perform a subscribe/set/unset/unsubscribe sequence
     * 
     * @throws Exception
     */
    @Test
    public void test2 () throws Exception
    {
        final SubscriptionRecorder<String> recorder = new SubscriptionRecorder<String> ();
        final SubscriptionSourceTestImpl<String> source = new SubscriptionSourceTestImpl<String> ();

        this.manager.subscribe ( "", recorder );
        this.manager.setSource ( "", source );
        this.manager.setSource ( "", null );
        this.manager.unsubscribe ( "", recorder );

        Assert.assertArrayEquals ( "Events are not the same", new Object[] { new SubscriptionStateEvent ( SubscriptionState.GRANTED ), new SubscriptionStateEvent ( SubscriptionState.CONNECTED ), new SubscriptionSourceEvent<String> ( true, source ), new SubscriptionSourceEvent<String> ( false, source ), new SubscriptionStateEvent ( SubscriptionState.GRANTED ), new SubscriptionStateEvent ( SubscriptionState.DISCONNECTED ) }, recorder.getList ().toArray ( new Object[0] ) );

        Assert.assertEquals ( "Number of subscriptions does not match", 0, this.manager.getSubscriptionCount () );
    }

    /**
     * Perform a common subscribe/unsubscribe with a subscriptions source being
     * present
     * before the subscription.
     * 
     * @throws Exception
     */
    @Test
    public void test3 () throws Exception
    {
        final SubscriptionRecorder<String> recorder = new SubscriptionRecorder<String> ();
        final SubscriptionSourceTestImpl<String> source = new SubscriptionSourceTestImpl<String> ();

        this.manager.setSource ( "", source );
        this.manager.subscribe ( "", recorder );
        this.manager.unsubscribe ( "", recorder );
        this.manager.setSource ( "", null );

        Assert.assertArrayEquals ( "Events are not the same", new Object[] { new SubscriptionStateEvent ( SubscriptionState.CONNECTED ), new SubscriptionSourceEvent<String> ( true, source ), new SubscriptionSourceEvent<String> ( false, source ), new SubscriptionStateEvent ( SubscriptionState.DISCONNECTED ) }, recorder.getList ().toArray ( new Object[0] ) );

        Assert.assertEquals ( "Number of subscriptions does not match", 0, this.manager.getSubscriptionCount () );
    }

    /**
     * Perform a common subscribe/unsubscribe with a subscriptions source being
     * present
     * before the subscription but loosing the subscription source while beeing
     * connected.
     * 
     * @throws Exception
     */
    @Test
    public void test4 () throws Exception
    {
        final SubscriptionRecorder<String> recorder = new SubscriptionRecorder<String> ();
        final SubscriptionSourceTestImpl<String> source = new SubscriptionSourceTestImpl<String> ();

        this.manager.setSource ( "", source );
        this.manager.subscribe ( "", recorder );
        this.manager.setSource ( "", null );
        this.manager.unsubscribe ( "", recorder );

        Assert.assertArrayEquals ( "Events are not the same", new Object[] { new SubscriptionStateEvent ( SubscriptionState.CONNECTED ), new SubscriptionSourceEvent<String> ( true, source ), new SubscriptionSourceEvent<String> ( false, source ), new SubscriptionStateEvent ( SubscriptionState.GRANTED ), new SubscriptionStateEvent ( SubscriptionState.DISCONNECTED ) }, recorder.getList ().toArray ( new Object[0] ) );

        Assert.assertEquals ( "Number of subscriptions does not match", 0, this.manager.getSubscriptionCount () );
    }

    /**
     * Test the method {@link SubscriptionManager#getAllGrantedTopics()}
     * 
     * @throws Exception
     */
    @Test
    public void test5 () throws Exception
    {
        final SubscriptionRecorder<String> recorder = new SubscriptionRecorder<String> ();

        this.manager.subscribe ( "1", recorder );
        this.manager.subscribe ( "2", recorder );

        final List<String> topics = new LinkedList<String> ();
        topics.add ( "1" );
        topics.add ( "2" );
        Collections.sort ( topics );

        final List<String> actualTopics = new ArrayList<String> ( this.manager.getAllGrantedTopics () );
        Collections.sort ( actualTopics );

        Assert.assertEquals ( "Topics do not match", topics, actualTopics );

        this.manager.unsubscribe ( "1", recorder );
        this.manager.unsubscribe ( "2", recorder );

        Assert.assertEquals ( "Topics do not match", new HashSet<String> (), this.manager.getAllGrantedTopics () );
    }
}
