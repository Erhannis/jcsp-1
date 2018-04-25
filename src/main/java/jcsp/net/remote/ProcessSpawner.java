
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

package jcsp.net.remote;

import java.io.*;
import jcsp.lang.*;
import jcsp.util.ints.*;
import jcsp.net.*;

/**
 * Launches a new JVM to run a process received by this spawner. This process will communicate with the
 * new JVM, routing any exceptions to the requesting host and also any information printed to the
 * <code>System.err</code> or <code>System.out</code> streams.
 *
 *
 * @author Quickstone Technologies Limited
 */
class ProcessSpawner implements CSProcess
{
   /** A <code>String</code> follows with a line of text that was for <code>System.out</code>. */
   final static int MSG_STDOUT = 0; // string follows
   /** A <code>String</code> follows with a line of text that was for <code>System.err</code>. */
   final static int MSG_STDERR = 1; // string follows
   /** An <code>Exception</code> follows that was raised by the spawned process. */
   final static int MSG_EXCEPTION = 2; // exception follows
   /** An <code>Exception</code> follows that was raised by the spawned process on failure. */
   final static int MSG_FAIL = 4; // exception follows
   /** The process terminated without error. */
   final static int MSG_OK = 5; // nothing follows
   
   /** A <code>String</code> follows with a line of text that was for <code>System.out</code>. */
   private final static Integer msgSTDOUT = new Integer(MSG_STDOUT);
   /** A <code>String</code> follows with a line of text that was for <code>System.err</code>. */
   private final static Integer msgSTDERR = new Integer(MSG_STDERR);
   /** An <code>Exception</code> follows that was raised by the spawned process. */
   private final static Integer msgEXCEPTION = new Integer(MSG_EXCEPTION);
   /** An <code>Exception</code> follows that was raised by the spawned process on failure. */
   private final static Integer msgFAIL = new Integer(MSG_FAIL);
   /** The process terminated without error. */
   private final static Integer msgOK = new Integer(MSG_OK);
   
   /** The parent service that started this one. */
   private final SpawnerService service;
   /** The process that needs to be started. */
   private final CSProcess process;
   /** For sending data back to the calling JVM who is running the <code>RemoteProcess</code> proxy. */
   private final NetChannelOutput caller;
   /**
    * The factory the child should use to initialize its node or <code>null</code> if the default initialization
    * should take place.
    */
   private final NodeFactory factory;
   /** The application identifier of the caller that the child should adopt. */
   private final ApplicationID applicationID;
   /** A unique name generated by the service fo use in creating a temporary file. */
   private final String uniqueName;
   /**
    * The classpath the spawned JVM should use to get the caller's classes from a networked
    * filesystem or <code>null</code> if the default classpath should be used.
    */
   private final String classPath;
   
   /**
    * Constructs a new spawner.
    *
    * @param service the parent service that is creating this object.
    * @param process the process that should be run.
    * @param caller for sending data back to the caller.
    * @param factory optional factory for initializing the child process' node.
    * @param applicationID caller's application ID that the child should adopt.
    * @param unique a unique number allocated by the parent service.
    * @param classPath classpath specified by the caller process for any classes available from a
    *                  networked filesystem.
    */
   public ProcessSpawner(SpawnerService service, CSProcess process, NetChannelOutput caller, 
                         NodeFactory factory, ApplicationID applicationID, int unique, String classPath)
   {
      this.service = service;
      this.process = process;
      this.caller = caller;
      this.factory = factory;
      this.applicationID = applicationID;
      String str = Node.getInstance().getNodeID().toString();
      str = str.replace(' ', '_').replace('\\', '_').replace('.', '_').replace(':', '_');
      this.uniqueName = str + unique;
      this.classPath = classPath;
   }
   
   /**
    * Main process loop.
    */
   public void run()
   {
      final Throwable eToThrow[] = new Throwable[] { null };
      try
      {
         final NetAltingChannelInput _in = NetChannelEnd.createNet2One();
         File f = File.createTempFile(uniqueName, "bin");
         ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
         os.writeObject(factory);
         os.flush();
         os.writeObject(process);
         os.flush();
         os.writeObject(applicationID);
         os.flush();
         os.writeObject(_in.getChannelLocation());
         os.close();
         
         // Start the child process
         Node.info.log(this, "Starting child process " + f.getAbsolutePath());
         final Process child;
         if (classPath == null)
            child = Runtime.getRuntime().exec(new String[] {"java", ChildProcess.class.getName(), f.getAbsolutePath()});
         else
            child = Runtime.getRuntime().exec(new String[] 
                                             {"java", "-cp", classPath, ChildProcess.class.getName(), f.getAbsolutePath()});
         Node.info.log(this, "Child process " + f.getAbsolutePath() + " started");
 
         // A semaphore for synchronising access to the caller
         final Any2OneChannelInt semaphore = Channel.any2oneInt(new BufferInt(1));
         
         // Trap the STDOUT and STDERR
         Parallel par = new Parallel(new CSProcess[] 
                                    {
                                       new CSProcess()
                                       {
                                          public void run()
                                          { 
                                             try
                                             { 
                                                boolean claimed = false;
                                                try
                                                { 
                                                   InputStream in = child.getErrorStream();
                                                   Reader reader = new InputStreamReader(in);
                                                   BufferedReader buffReader = new BufferedReader(reader);
                                                   String line = buffReader.readLine();
                                                   while (line != null)
                                                   { 
                                                      semaphore.out().write(0);
                                                      claimed = true;
                                                      caller.write(msgSTDERR);
                                                      caller.write(line);
                                                      semaphore.in().read();
                                                      claimed = false;
                                                      line = buffReader.readLine();
                                                   }
                                                   buffReader.close();
                                                }
                                                catch (LinkLostException e)
                                                {
                                                   child.destroy();
                                                   semaphore.in().read();
                                                }
                                                catch (Exception e)
                                                {
                                                   if (!claimed)
                                                      semaphore.out().write(0);
                                                   caller.write(msgEXCEPTION);
                                                   caller.write(e);
                                                   semaphore.in().read();
                                                }
                                             }
                                             catch (Exception e)
                                             {
                                                eToThrow[0] = e;
                                             }
                                          }
                                       }, 
                                       new CSProcess()
                                       {
                                          public void run()
                                          {
                                             // Wait for an exception to be sent from the child, a null to be sent or
                                             // the process to terminate without sending either (an error)
                                             CSTimer tim = new CSTimer();
                                             Alternative alt = new Alternative(new Guard[] { _in, tim });
                                             while (true)
                                             {
                                                tim.setAlarm(tim.read() + 2000);
                                                if (alt.priSelect() == 0)
                                                {
                                                   eToThrow[0] = (Throwable) _in.read();
                                                   try
                                                   {
                                                      semaphore.out().write(0);
                                                      caller.write(null);
                                                   }
                                                   catch (Exception e)
                                                   {
                                                      child.destroy();
                                                   }
                                                   finally
                                                   {
                                                      semaphore.in().read();
                                                   }
                                                   break;
                                                }
                                                else
                                                {
                                                   try
                                                   {
                                                      int ec = child.exitValue();
                                                      eToThrow[0] = new RemoteProcessFailedException(ec, process);
                                                      break;
                                                   }
                                                   catch (IllegalThreadStateException e)
                                                   {
                                                   }
                                                }
                                             }
                                          }
                                       }, 
                                       new CSProcess()
                                       {
                                          public void run()
                                          {
                                             try
                                             {
                                                boolean claimed = false;
                                                try
                                                {
                                                   InputStream in = child.getInputStream();
                                                   Reader reader = new InputStreamReader(in);
                                                   BufferedReader buffReader = new BufferedReader(reader);
                                                   String line = buffReader.readLine();
                                                   while (line != null)
                                                   {
                                                      semaphore.out().write(0);
                                                      claimed = true;
                                                      caller.write(msgSTDOUT);
                                                      caller.write(line);
                                                      semaphore.in().read();
                                                      claimed = false;
                                                      line = buffReader.readLine();
                                                   }
                                                   buffReader.close();
                                                }
                                                catch (LinkLostException e)
                                                {
                                                   child.destroy();
                                                   semaphore.in().read();
                                                }
                                                catch (Exception e)
                                                {
                                                   if (!claimed)
                                                      semaphore.out().write(0);
                                                   caller.write(msgEXCEPTION);
                                                   caller.write(e);
                                                   semaphore.in().read();
                                                }
                                             }
                                             catch (Exception e)
                                             {
                                                eToThrow[0] = e;
                                             }
                                          }
                                       }
                                    });
         par.run();
         par.releaseAllThreads();
         
         // wait for process to terminate
         Node.info.log(this, "Child process " + f.getAbsolutePath() + " waiting to terminate");
         int ec = child.waitFor();
         Node.info.log(this, "Child process " + f.getAbsolutePath() + " terminated");
         if (ec != 0)
            if (eToThrow[0] == null)
               eToThrow[0] = new RemoteProcessFailedException(ec, process);
      }
      catch (Exception e)
      {
         eToThrow[0] = e;
      }
      try
      {
         if (eToThrow[0] != null)
         {
            caller.write(msgFAIL);
            caller.write(eToThrow[0]);
         }
         else
            caller.write(msgOK);
      }
      finally
      {
         caller.destroyWriter();
      }  
   }
}
