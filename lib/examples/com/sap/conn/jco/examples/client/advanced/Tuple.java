package com.sap.conn.jco.examples.client.advanced;

/** 
 * Simple helper class for associating a transaction instance with some data.
 */
public class Tuple<X, Y>
{
    public final X jcoFunctionContainer;
    public final Y dataString;

    public Tuple(X x, Y y)
    {
        jcoFunctionContainer=x;
        dataString=y;
    }
}
