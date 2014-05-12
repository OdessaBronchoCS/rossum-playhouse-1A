/*  -------------------------------------------------------------

 Rossum's Playhouse  --  a client/server based robot simulator
 Rossum's Playhouse is also known under the name "RP1".
 Copyright (C) 1999  G.W. Lucas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 ----------------------------------------------------------------- */
/*

 TO DO:  Replace array-based management of client list with one
 of the Java container classes, such as ArrayList used in RsBody


 TO DO:  I have changed the way state data is transferred between objects.
 An explanation of how it works should be added to the comments,
 especially in terms of synchronization and object conservation

 */
package rp1.simulator;

import rp1.rossum.*;
import rp1.rossum.event.*;
import rp1.planparser.RsPlanReader;
import rp1.planparser.RsParsingException;


import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * The main class providing session data and related methods.
 */
public class SimSession extends Thread implements RsLogInterface, RsInterlock, SimStateDataInterface {

    protected SimProperties properties;
    protected SimScheduler scheduler;
    protected SimFrame simFrame;
    private Random randomKeyMaker;
    private int serialNumber;
    private PrintWriter logWriter;
    private DateFormat logDateFormat;
    private StringBuffer logBuffer;
    private RsPlan plan;
    protected double animationFrameInterval;
    protected double modelingFrameInterval;
    protected SimClient[] clients;
    protected RsBody[] bodyArray;
    protected ArrayList<SimPaintBox> paintBoxArrayList;
    protected SimPaintBox[] paintBoxArray;
    private SimMotionTask motionTask;
    protected SimStateDataExchange stateDataExchange;
    private int interlock;

    public SimSession(SimProperties properties) {

        this.properties = properties;
        randomKeyMaker = new Random();
        serialNumber = 0;
        scheduler = new SimScheduler();
        simFrame = null;
        clients = null;
        motionTask = new SimMotionTask(this);
        stateDataExchange = new SimStateDataExchange();

        paintBoxArrayList = new ArrayList<>();

        animationFrameInterval = 1.0 / properties.animationFrameRate;
        if (animationFrameInterval < 1.0 / 30.0) {
            animationFrameInterval = 1.0 / 30.0;  // max is 30 frames per second
        }
        modelingFrameInterval = 1.0 / properties.modelingFrameRate;
        if (modelingFrameInterval < 0.001) {
            modelingFrameInterval = 0.001;
        }

        if (properties.logToFile) {
            try {
                logWriter = new PrintWriter(
                        new BufferedWriter(
                        new FileWriter(properties.logFileName)));
            } catch (IOException e) {
                throw new Error("Fatal Error -- unable to open log file \"" + properties.logFileName + "\"");
                //  System.exit(-1);
            }
            System.err.println("Now Logging to file " + properties.logFileName);
        } else {
            logWriter = null;
        }
        logDateFormat = new SimpleDateFormat("h:mm:ss.SSS");
        logBuffer = new StringBuffer();
        logIt("X", "STARTING SIMULATOR SESSION");


        if (properties.dlcEnabled) {
            if (properties.dlcName == null) {
                logIt("X", "Fatal Error -- Properties specify DLC Enabled, but no class name provided");
                System.exit(-1);
            }

            if (!properties.dlcSetIO && !properties.isSocketEnabled()) {
                logIt("X", "Fatal Error -- Properties disabled setting DLC IO, but Network Connections are disabled");
                System.exit(-1);
            }
        }


        String fpName = properties.getFloorPlanFileName();
        if (fpName == null) {
            logIt("X", "Fatal Error -- floor plan name spacification is missing");
            System.exit(-1);
        }

        int slashIndex = fpName.indexOf('/');
        InputStream planStream = null;

        if (slashIndex == -1) {
            // there's no slash, read it from the CLASSPATH
            log("Loading floor plan from resource (file) FloorPlans/" + fpName);
            ClassLoader mainLoader = properties.mainClass.getClassLoader();
            URL planURL = mainLoader.getResource("FloorPlans/" + fpName);
            planStream = null;
            if (planURL != null) {
                logIt("X", "Reading floorplan from " + planURL);
                planStream = mainLoader.getResourceAsStream("FloorPlans/" + fpName);
            }
            if (planStream == null) {
                logIt("X", "Fatal error trying to get plan resource (file: " + fpName + ")");
                System.exit(-1);
            }
        } else {
            log("Loading floor plan from file " + properties.getFloorPlanFileName());
            try {
                planStream = new FileInputStream(fpName);
            } catch (IOException ep) {
                logIt("X", "Fatal error trying to read plan file: " + fpName + "\n\t" + ep.toString());
                System.exit(-1);
            }
        }


        RsPlanReader reader = new RsPlanReader(fpName);
        try {
            plan = reader.readPlan(planStream);
        } catch (RsParsingException | IOException eParse) {
            logIt("X", "Fatal Error attempting to read plan\n" + eParse.toString());
            System.exit(-1);
        }
    }

    @Override
    public void run() {

        if (properties.isGuiEnabled()) {
            log("Starting GUI");
            simFrame = new SimFrame(this);
            simFrame.setVisible(true);
        } else {
            log("GUI is disabled (in response to configuration option)");
        }


        if (properties.isSocketEnabled()) {
            scheduler.add(new SimClientListenerTask(this));
        } else {
            log("Network and local client connections disabled (in response to configuration option)");
        }

        if (properties.dlcEnabled) {
            log("Queuing task to launch client " + properties.dlcName);
            scheduler.add(new SimClientLauncherTask(
                    this,
                    properties.dlcName,
                    properties.dlcSetIO,
                    properties.dlcSetLog));
        }


        log("Starting main scheduler loop with simulation speed: " + properties.getSimulationSpeed());
        log("modelingFrameInterval (sec): " + modelingFrameInterval);
        log("modeling sample rate (Hz): : " + (1.0 / modelingFrameInterval));


        scheduler.setSimSpeed(properties.getSimulationSpeed());
        motionTask.setSimSpeed(scheduler.getSimSpeed());

        scheduler.startClock();
        SimTask task;
        while (true) {
            task = scheduler.waitForNextTask();
            task.process();
        }
    }

    protected void terminate() {
        // terminate the whole application
        // TO DO:  add more statistics and information...
        //         about tasks, paintboxes, clients, etc.
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        log("Shutting down simulator");
        logPrintln("  Total Memory used by JVM: " + totalMemory);
        logPrintln("  Free  Memory:             " + freeMemory);
        System.exit(0);
    }

    public synchronized int getNewKey() {
        return randomKeyMaker.nextInt();
    }

    public synchronized int getNewSerialNumber() {
        serialNumber++;
        return serialNumber;
    }

    public void addSessionElementsToClient(SimClient client) {

        client.setLogger(this);

        // add the request handlers supported by the simulator
        client.setBodyHandler(new SimBodyHandler(client));
        client.setMotionRequestHandler(new SimMotionRequestHandler(client));
        client.setTimeoutRequestHandler(new SimTimeoutRequestHandler(client));
        client.setProtocolShutdownHandler(new SimProtocolShutdownHandler(client));
        client.setPositionRequestHandler(new SimPositionRequestHandler(client));
        client.setSensorRequestHandler(new SimSensorRequestHandler(client));
        client.setPlacementRequestHandler(new SimPlacementRequestHandler(client));
        client.setHaltRequestHandler(new SimHaltRequestHandler(client));
        client.setTargetSelectionRequestHandler(new SimTargetSelectionRequestHandler(client));
        client.setPlanRequestHandler(new SimPlanRequestHandler(client));
        client.setHeartbeatRequestHandler(new SimHeartbeatRequestHandler(client));
        client.setPainterChangeRequestHandler(new SimPainterChangeRequestHandler(client));
        client.setActuatorControlRequestHandler(new SimActuatorControlRequestHandler(client));
        client.setEncoderStatusRequestHandler(new SimEncoderStatusRequestHandler(client));
    }

    public void queueMotionTask() {
        // IMPORTANT:  this method must not be called by anything
        // except by threads in the main scheduler thread.
        // This restriction ensures that the motionTask will
        // never be processing when this method is called.
        // Note that this method is NOT synchronized.  By strict adherence to
        // the rule about what calls this, we avoid the need for synchronization
        // and its attendent overhead.
        if (!scheduler.isTaskOnQueue(motionTask)) {
            scheduler.addTaskAtUpdatedSimTime(motionTask);
        }
    }

    public void queueAnimationEvent() {
        // IMPORTANT:  this method must not be called by anything
        // except by objects in the main scheduler thread.
        // Note that this method is NOT synchronized.
        // ALL requests to queue animation events must go through SimSession.
        //
        // It would be easy to implement multiple
        // graphics frames (containing multiple views of the same
        // simulation at different scales, zooms, or whatever)
        // Just change this module to access an array of SimFrames
        // rather than just one.
        if (simFrame != null) {
            stateDataExchange.storeStateData(this);
            simFrame.canvas.queueAnimationEvent();
        }
    }

    public void queueRepaintEvent() {
        if (simFrame != null) {
            stateDataExchange.storeStateData(this);
            simFrame.canvas.repaint();
        }
    }

    public synchronized void addClient(SimClient client) {
        paintBoxArray = null;
        bodyArray = null;
        if (clients == null) {
            clients = new SimClient[1];
            clients[0] = client;
        } else {
            SimClient[] c = new SimClient[clients.length + 1];
            System.arraycopy(clients, 0, c, 0, clients.length);
            c[clients.length] = client;
            clients = c;
        }
    }

    public synchronized void removeClient(SimClient client) {
        bodyArray = null;
        if (clients == null) {
            return;
        }

        int i;
        for (i = 0; i < clients.length; i++) {
            if (clients[i] == client) {
                break;
            }
        }

        if (i == clients.length) {
            // the client wasn't on list anyway, I don't expect this to happen
            return;
        }

        if (clients.length == 1) {
            // there are no more clients.  Any motionTask
            // in the queue is no longer needed.
            clients = null;
            scheduler.remove(motionTask);
        } else {
            SimClient[] c = new SimClient[clients.length - 1];
            int n = 0;
            for (i = 0; i < clients.length; i++) {
                if (clients[i] == client) {
                    continue;
                }
                c[n++] = clients[i];
            }
            clients = c;
        }
        queueAnimationEvent();

        int maxInterlockSent;
        maxInterlockSent = client.getMaxInterlockSent();
        if (maxInterlockSent > 0) {
            closeInterlock(maxInterlockSent);
        }
    }

    // stuff related to logging -----------------------------------------
    public String fmtElapsedTime(double time) {
        StringBuilder sBuffer = new StringBuilder();
        String sSeconds;
        String sMillis;

        // constrain a delta time to the range [0,10000.0]
        double deltaT = time - 10000.0 * Math.floor(time / 10000);
        int seconds = (int) Math.floor(deltaT);
        int millis = (int) Math.floor((deltaT - seconds) * 1000.0);

        sSeconds = (new Integer(seconds)).toString();
        for (int i = sSeconds.length(); i < 4; i++) {
            sBuffer.append("0");
        }
        sBuffer.append(sSeconds);

        sBuffer.append(".");
        sMillis = (new Integer(millis)).toString();
        for (int i = sMillis.length(); i < 3; i++) {
            sBuffer.append("0");
        }
        return sBuffer.toString();
    }

    @Override
    public void logIt(String level, String message) {
        double simTime = scheduler.getSimTime();
        int i;
        String sSeconds;
        String sMillis;

        logBuffer.setLength(0);
        logBuffer.append(level).append(" ").append(logDateFormat.format(new Date())).append(" ");

        // constrain a delta time to the range [0,10000.0]
        double deltaT = simTime - 10000.0 * Math.floor(simTime / 10000);
        int seconds = (int) Math.floor(deltaT);
        int millis = (int) Math.floor((deltaT - seconds) * 1000.0);

        sSeconds = (new Integer(seconds)).toString();
        for (i = sSeconds.length(); i < 4; i++) {
            logBuffer.append("0");
        }
        logBuffer.append(sSeconds);

        logBuffer.append(".");
        sMillis = (new Integer(millis)).toString();
        for (i = sMillis.length(); i < 3; i++) {
            logBuffer.append("0");
        }
        logBuffer.append(sMillis).append(": ").append(message);

        String s = logBuffer.toString();
        if (logWriter != null) {
            logWriter.println(s);
            logWriter.flush();
        }
        if (properties.logToSystemOut) {
            System.out.println(s);
        }
    }

    @Override
    public void log(String message) {
        logIt("s", message);
    }

    @Override
    public void verbose(String message) {
        // DEVELOPMENT NOTE...   I don't know if this is significant.   The reason
        // that verbose is not synchronized is that I did not want to incur the
        // overhead associated with synchronization if the verbose logging was OFF.
        // If verbose logging is on, nobody would care, but if performance becomes an
        // issue, I don't want the vertigial "verbose" method invokations logging to be significant
        if (properties.logVerbose) {
            logIt("v", message);
        }
    }

    @Override
    public synchronized boolean getVerbosity() {
        return properties.logVerbose;
    }

    @Override
    public synchronized void setVerbosity(boolean value) {
        properties.logVerbose = value;
    }

    public synchronized void logPrintln(String message) {
        // write a plain string to log
        if (logWriter != null) {
            logWriter.println(message);
            logWriter.flush();
        }
        if (properties.logToSystemOut) {
            System.out.println(message);
        }
    }

    public synchronized RsPlan getPlan() {
        return plan;
    }

    public void sendMouseClickEvent(RsMouseClickEvent mce) {
        if (clients == null) {
            return;
        }
        for (int i = 0; i < clients.length; i++) {
            clients[i].sendMouseClickEvent(mce);
        }
    }

    @Override
    public synchronized int openInterlock() {
        scheduler.stopClock();
        interlock++;
        if (properties.logVerbose) {
            logIt("v", "open interlock " + interlock);
        }
        return interlock;
    }

    @Override
    public synchronized boolean closeInterlock(int interlockResponse) {
        if (properties.logVerbose) {
            logIt("v", "close interlock " + interlockResponse + "      (" + interlock + ")");
        }
        if (interlockResponse >= interlock) {
            scheduler.startClock();
            interlock = interlockResponse;
            return true;
        }
        return false;
    }

    @Override
    public synchronized int getMaximumInterlockIndex() {
        return interlock;
    }

    public synchronized void addPaintBox(SimPaintBox pbx) {
        paintBoxArrayList.add(pbx);
        paintBoxArray = null;
    }

    // methods required to support SimStateDataInterface and transfer of state data
    // note that the setPaintBoxArray and setBodyArray methods are implemented
    // as do-nothing methods in this class.
    @Override
    public synchronized SimPaintBox[] getPaintBoxArray() {
        if (paintBoxArray == null) {
            if (paintBoxArrayList.size() > 0) {
                paintBoxArray =
                        (SimPaintBox[]) paintBoxArrayList.toArray(new SimPaintBox[paintBoxArrayList.size()]);
            }
        }
        return paintBoxArray;
    }

    public RsBody[] getBodyArray() {
        if (bodyArray == null) {
            if (clients != null && clients.length > 0) {
                bodyArray = new RsBody[clients.length];
                for (int i = 0; i < clients.length; i++) {
                    bodyArray[i] = clients[i].body;
                }
            }
        }
        return bodyArray;
    }

    public void setPaintBoxArray(SimPaintBox[] a) {
    }

    public void setBodyArray(RsBody[] b) {
    }

    public synchronized void resetPaintBoxes() {
        paintBoxArrayList.clear();
        if (clients == null) {
            return;
        }

        for (int iClient = 0; iClient < clients.length; iClient++) {
            SimPaintBox[] pb = clients[iClient].getPaintBoxArray();
            if (pb == null) {
                continue;
            }
            for (int i = 0; i < pb.length; i++) {
                paintBoxArrayList.add(pb[i]);
            }
        }
    }
}
