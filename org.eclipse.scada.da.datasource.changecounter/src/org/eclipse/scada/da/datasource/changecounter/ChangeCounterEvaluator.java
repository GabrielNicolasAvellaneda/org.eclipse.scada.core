package org.eclipse.scada.da.datasource.changecounter;

import java.util.List;

import org.eclipse.scada.core.Variant;
import org.eclipse.scada.da.datasource.data.DataItemValueLight;
import org.eclipse.scada.da.datasource.data.DataItemValueRange;

class ChangeCounterEvaluator
{
    static int handleDelta ( List<Variant> values, DataItemValueRange valueRange, ErrorHandling errorHandling )
    {
        Double delta = values.get ( 0 ).asDouble ( null );
        int numOfChanges = 0;
        // we **have** to consider the value before, since if it is a change within this hour, it is a change respective to the initial state
        Double lastValue = ( valueRange.getState ().getFirstValue ().isError () || !valueRange.getState ().getFirstValue ().hasValue () ) ? null : valueRange.getState ().getFirstValue ().getValue ().asDouble ( null );
        String error = null;
        for ( DataItemValueLight v : valueRange.getState ().getValues () )
        {
            // first handle an error
            if ( v.isError () || !v.hasValue () )
            {
                if ( errorHandling == ErrorHandling.error )
                {
                    throw new IllegalArgumentException ( "value is invalid" );
                }
                else if ( errorHandling == ErrorHandling.count )
                {
                    if ( lastValue != null )
                    {
                        numOfChanges += 1;
                    }
                    lastValue = null;
                    continue;
                }
                else if ( errorHandling == ErrorHandling.ignore )
                {
                    continue;
                }
            }
            // by now, value has to be a number
            final Double currentValue = v.getValue ().asDouble ( null );
            if ( lastValue == null )
            {
                numOfChanges += 1;
            }
            else
            {
                if ( Math.abs ( lastValue - currentValue ) > delta )
                {
                    numOfChanges += 1;
                }
            }
            lastValue = currentValue;
        }
        return numOfChanges;
    }

    static int handleDirection ( List<Variant> values, DataItemValueRange valueRange, ErrorHandling errorHandling )
    {
        int numOfChanges = 0;
        // we **have** to consider the value before, since if it is a change within this hour, it is a change respective to the initial state
        Double lastValue = ( valueRange.getState ().getFirstValue ().isError () || !valueRange.getState ().getFirstValue ().hasValue () ) ? null : valueRange.getState ().getFirstValue ().getValue ().asDouble ( null );
        String error = null;
        int direction = 0;
        for ( DataItemValueLight v : valueRange.getState ().getValues () )
        {
            // first handle an error
            if ( v.isError () || !v.hasValue () )
            {
                if ( errorHandling == ErrorHandling.error )
                {
                    throw new IllegalArgumentException ( "value is invalid" );
                }
                else if ( errorHandling == ErrorHandling.count )
                {
                    if ( lastValue != null )
                    {
                        numOfChanges += 1;
                    }
                    lastValue = null;
                    direction = 0;
                    continue;
                }
                else if ( errorHandling == ErrorHandling.ignore )
                {
                    continue;
                }
            }
            // by now, value has to be a number
            final Double currentValue = v.getValue ().asDouble ( null );
            if ( lastValue == null )
            {
                numOfChanges += 1;
            }
            else
            {
                double delta = currentValue - lastValue;
                if ( delta > 0.0 )
                {
                    int newDirection = Long.valueOf ( Math.round ( delta / Math.abs ( delta ) ) ).intValue ();
                    if ( newDirection != direction )
                    {
                        numOfChanges += 1;
                    }
                    direction = newDirection;
                }
            }
            lastValue = currentValue;
        }
        return numOfChanges;
    }

    static int handleSet ( List<Variant> values, DataItemValueRange valueRange, ErrorHandling errorHandling )
    {
        int numOfChanges = 0;
        // we **have** to consider the value before, since if it is a change within this hour, it is a change respective to the initial state
        Variant lastValue = ( valueRange.getState ().getFirstValue ().isError () ) ? Variant.NULL : valueRange.getState ().getFirstValue ().getValue ();
        String error = null;
        for ( DataItemValueLight v : valueRange.getState ().getValues () )
        {
            if ( v.isError () )
            {
                if ( errorHandling == ErrorHandling.error )
                {
                    throw new IllegalArgumentException ( "value is invalid" );
                }
                else if ( errorHandling == ErrorHandling.count )
                {
                    if ( lastValue != Variant.NULL )
                    {
                        numOfChanges += 1;
                    }
                    lastValue = Variant.NULL;
                    continue;
                }
                else if ( errorHandling == ErrorHandling.ignore )
                {
                    continue;
                }
            }
            else
            {
                Variant value = v.getValue ();
                if ( values.contains ( value ) && !values.contains ( lastValue ) )
                {
                    numOfChanges += 1;
                }
                else if ( !values.contains ( value ) && values.contains ( lastValue ) )
                {
                    numOfChanges += 1;
                }
                lastValue = v.getValue ();
            }
        }
        return numOfChanges;
    }
}
