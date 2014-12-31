/*
    JPC-RR: A x86 PC Hardware Emulator
    Release 1

    Copyright (C) 2007-2009 Isis Innovation Limited
    Copyright (C) 2009-2010 H. Ilari Liusvaara

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Based on JPC x86 PC Hardware emulator,
    A project from the Physics Dept, The University of Oxford

    Details about original JPC can be found at:

    www-jpc.physics.ox.ac.uk

*/

package org.jpc.emulator.peripheral;

import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.*;
import org.jpc.output.*;


import java.io.*;

/**
 *
 * @author Chris Dennis
 * @author Ian Preston
 */
public class PCSpeaker extends AbstractHardwareComponent implements IOPortCapable
{
    private int dummyRefreshClock, mode;
    private IntervalTimer pit;
    private Clock clock;
    private boolean pitInput, ioportRegistered, lastState;
    private OutputChannelOBM soundOut;

    public PCSpeaker(Output out, String name)
    {
        ioportRegistered = false;
        mode = 0;
        pitInput = true;
        lastState= true;
        soundOut = new OutputChannelOBM(out, name);
        soundOut.addFrameOBM(0, false);
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        super.dumpStatusPartial(output);
        output.println("\tdummyRefreshClock " + dummyRefreshClock + " mode " + mode + " lastState " + lastState);
        output.println("\tioportRegistered " + ioportRegistered + " pitInput " + pitInput);
        output.println("\tpit <object #" + output.objectNumber(pit) + ">"); if(pit != null) pit.dumpStatus(output);
        output.println("\tclock <object #" + output.objectNumber(clock) + ">"); if(clock != null) clock.dumpStatus(output);
        output.println("\tsoundOut <object #" + output.objectNumber(soundOut) + ">"); if(soundOut != null) soundOut.dumpStatus(output);
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": PCSpeaker:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        super.dumpSRPartial(output);
        output.dumpInt(dummyRefreshClock);
        output.dumpInt(mode);
        output.dumpObject(pit);
        output.dumpObject(clock);
        output.dumpObject(soundOut);
        output.dumpBoolean(ioportRegistered);
        output.dumpBoolean(pitInput);
        output.dumpBoolean(lastState);
    }

    public PCSpeaker(SRLoader input) throws IOException
    {
        super(input);
        dummyRefreshClock = input.loadInt();
        mode = input.loadInt();
        pit = (IntervalTimer)input.loadObject();
        clock = (Clock)input.loadObject();
        soundOut = (OutputChannelOBM)input.loadObject();
        ioportRegistered = input.loadBoolean();
        pitInput = input.loadBoolean();
        lastState = input.loadBoolean();
    }

    public int[] ioPortsRequested()
    {
        return new int[]{0x61};
    }

    public int ioPortReadByte(int address)
    {
        int out = pit.getOut(2);
        dummyRefreshClock ^= 1;
        return mode | (out << 5) | (dummyRefreshClock << 4);
    }
    public int ioPortReadWord(int address)
    {
        return (0xff & ioPortReadByte(address)) |
            (0xff00 & (ioPortReadByte(address + 1) << 8));
    }
    public int ioPortReadLong(int address)
    {
        return (0xffff & ioPortReadWord(address)) |
            (0xffff0000 & (ioPortReadWord(address + 2) << 16));
    }

    public synchronized void ioPortWriteByte(int address, int data)
    {
        pit.setGate(2, (data & 1) != 0);
        mode = data & 3;
        updateSpeaker();
    }

    public void ioPortWriteWord(int address, int data)
    {
        this.ioPortWriteByte(address, data);
        this.ioPortWriteByte(address + 1, data >> 8);
    }
    public void ioPortWriteLong(int address, int data)
    {
        this.ioPortWriteWord(address, data);
        this.ioPortWriteWord(address + 2, data >> 16);
    }

    public void setPITInput(boolean in)
    {
        pitInput = in;
        updateSpeaker();
    }

    private void updateSpeaker()
    {
        //FIXME: I assume that pulses from PIT are negative polarity,
        //and that speaker line is clamped high when off. Is this
        //correct? Also is forcing line low when on but not connected
        //right?
        boolean line;
        if((mode & 2) == 0)
            line = true;    //Speaker off.
        else if((mode & 1) == 0)
            line = false;     //Speaker on and not following PIT.
        else
            line = pitInput; //Following PIT.
        long time = clock.getTime();
        if(line != lastState) {
            soundOut.addFrameOBM(time, lastState);
            soundOut.addFrameOBM(time, line);
        }
        lastState = line;
    }

    public boolean initialised()
    {
        return ioportRegistered && (pit != null) && (clock != null);
    }

    public void reset()
    {
        pit = null;
        ioportRegistered = false;
        clock = null;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof IntervalTimer) &&
            component.initialised()) {
            pit = (IntervalTimer)component;
        }
        if ((component instanceof Clock) &&
            component.initialised()) {
            clock = (Clock)component;
        }
        if ((component instanceof IOPortHandler)
            && component.initialised()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }
}
