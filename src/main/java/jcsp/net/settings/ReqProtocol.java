
//////////////////////////////////////////////////////////////////////
//                                                                  //
//  JCSP ("CSP for Java") Libraries                                 //
//  Copyright (C) 1996-2018 Peter Welch, Paul Austin and Neil Brown //
//                2001-2004 Quickstone Technologies Limited         //
//                2005-2018 Kevin Chalmers                          //
//                                                                  //
//  You may use this work under the terms of either                 //
//  1. The Apache License, Version 2.0                              //
//  2. or (at your option), the GNU Lesser General Public License,  //
//       version 2.1 or greater.                                    //
//                                                                  //
//  Full licence texts are included in the LICENCE file with        //
//  this library.                                                   //
//                                                                  //
//  Author contacts: P.H.Welch@kent.ac.uk K.Chalmers@napier.ac.uk   //
//                                                                  //
//////////////////////////////////////////////////////////////////////

package jcsp.net.settings;

/**
 * Used internally within the JCSP network infrastructure to represent a protocol requirement.
 *
 *
 * @author Quickstone Technologies Limited
 */
public class ReqProtocol extends Spec implements Req, XMLConfigConstants
{
   ReqProtocol(String protocolid)
   {
      super(REQ_NAME_PROTOCOL, true);
      this.protocolid = protocolid;
   }
   
   public String getStringValue()
   {
      return protocolid;
   }
   
   public String getComparator()
   {
      return REQ_COMPARATOR_EQUALS;
   }
   
   public int getIntValue()
   {
      throw new UnsupportedOperationException("Type is string");
   }
   
   public double getDoubleValue()
   {
      throw new UnsupportedOperationException("Type is string");
   }
   
   public boolean getBooleanValue()
   {
      throw new UnsupportedOperationException("Type is string");
   }
   
   public Class getType()
   {
      return String.class;
   }
   
   public String getValue()
   {
      return protocolid;
   }
   
   public boolean equals(Object o)
   {
      if(o instanceof ReqProtocol)
      {
         ReqProtocol other = (ReqProtocol) o;
         return protocolid.equals(other.protocolid);
      }
      return false;
   }
   
   public int hashCode()
   {
      return protocolid.hashCode();
   }
   
   private String protocolid;
}