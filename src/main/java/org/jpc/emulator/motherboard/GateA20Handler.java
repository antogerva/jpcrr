/*
    JPC-RR: A x86 PC Hardware Emulator
    Release 1

    Copyright (C) 2007-2009 Isis Innovation Limited
    Copyright (C) 2009 H. Ilari Liusvaara

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

package org.jpc.emulator.motherboard;

import java.io.*;

import org.jpc.emulator.*;
import org.jpc.emulator.memory.PhysicalAddressSpace;
import org.jpc.emulator.processor.Processor;

/**
 * I/O Device mapped to port 0x92 that controls the enabled status of the 20th
 * address line.
 * @author Chris Dennis
 */
public class GateA20Handler extends AbstractHardwareComponent implements IOPortCapable
{
    private Processor cpu;
    private PhysicalAddressSpace physicalAddressSpace;
    private boolean ioportRegistered;

    public GateA20Handler()
    {
        ioportRegistered = false;
        cpu = null;
        physicalAddressSpace = null;
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        super.dumpStatusPartial(output);
        output.println("\tioportRegistered " + ioportRegistered);
        output.println("\tcpu <object #" + output.objectNumber(cpu) + ">"); if(cpu != null) cpu.dumpStatus(output);
        output.println("\tphysicalAddressSpace <object #" + output.objectNumber(physicalAddressSpace) + ">"); if(physicalAddressSpace != null) physicalAddressSpace.dumpStatus(output);
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        output.dumpBoolean(ioportRegistered);
        output.dumpObject(cpu);
        output.dumpObject(physicalAddressSpace);
    }

    public GateA20Handler(SRLoader input) throws IOException
    {
        super(input);
        ioportRegistered = input.loadBoolean();
        cpu = (Processor)input.loadObject();
        physicalAddressSpace = (PhysicalAddressSpace)input.loadObject();
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": GateA20Handler:");
        dumpStatusPartial(output);
        output.endObject();
    }

    private void setGateA20State(boolean value)
    {
        physicalAddressSpace.setGateA20State(value);
    }

    /**
     * Writes a byte into the handler.  Bit 1 controls the A20 state, if high
     * A20 is enabled, if low A20 is disabled.  If bit 0 is high then the
     * processor will be reset.
     * @param address location being written to
     * @param data byte value being written
     */
    public void ioPortWriteByte(int address, int data)
    {
        setGateA20State((data & 0x02) != 0);
        if ((data & 0x01) != 0)
            cpu.reset();
    }
    public void ioPortWriteWord(int address, int data)
    {
        ioPortWriteByte(address, data);
    }
    public void ioPortWriteLong(int address, int data)
    {
        ioPortWriteByte(address, data);
    }

    /**
     * Reads a byte from the handler.  If A20 is enabled then this will return
     * 0x02, else it will return 0x00.
     * @param address location being read
     * @return byte value read
     */
    public int ioPortReadByte(int address)
    {
        return physicalAddressSpace.getGateA20State() ? 0x02 : 0x00;
    }
    public int ioPortReadWord(int address)
    {
        return ioPortReadByte(address) | 0xff00;
    }
    public int ioPortReadLong(int address)
    {
        return ioPortReadByte(address) | 0xffffff00;
    }

    public int[] ioPortsRequested()
    {
        return new int[] {0x92};
    }

    public boolean initialised()
    {
        return ioportRegistered && (cpu != null) && (physicalAddressSpace != null);
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof IOPortHandler) && component.initialised())
        {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }

        if (component instanceof PhysicalAddressSpace)
            physicalAddressSpace = (PhysicalAddressSpace)component;

        if ((component instanceof Processor) && component.initialised())
            cpu = (Processor) component;
    }

    public void reset()
    {
        ioportRegistered = false;
        physicalAddressSpace = null;
        cpu = null;
    }
}
