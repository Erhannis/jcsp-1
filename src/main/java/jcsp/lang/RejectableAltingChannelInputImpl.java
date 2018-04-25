
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

package jcsp.lang;

    class RejectableAltingChannelInputImpl extends RejectableAltingChannelInput {
	
	private ChannelInternals channel;
	private int immunity;
	
	RejectableAltingChannelInputImpl(ChannelInternals _channel, int _immunity) {
		channel = _channel;
		immunity = _immunity;
	}
	
	
	public boolean pending() {
		return channel.readerPending();
	}
	
	boolean disable() {
		return channel.readerDisable();
	}

	boolean enable(Alternative alt) {
		return channel.readerEnable(alt);
	}

	public void endRead() {
		channel.endRead();
	}

	public Object read() {
		return channel.read();
	}

	public Object startRead() {
		return channel.startRead();
	}

	public void poison(int strength) {
		if (strength > immunity) {
			channel.readerPoison(strength);
		}
	}


	public void reject()
    {
    	channel.readerPoison(Integer.MAX_VALUE);
    }
}
