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


package org.jpc.emulator.processor;

import java.io.IOException;
import org.jpc.emulator.memory.AddressSpace;
import org.jpc.emulator.SRLoader;
import org.jpc.emulator.SRDumper;
import org.jpc.emulator.SRDumpable;
import org.jpc.emulator.StatusDumper;

/**
 *
 * @author Chris Dennis
 */
public abstract class Segment implements SRDumpable
{
    protected AddressSpace memory;

    public Segment(AddressSpace memory, boolean dummy)
    {
        this.memory = memory;
    }

    public final void setAddressSpace(AddressSpace memory)
    {
        this.memory = memory;
    }

    public abstract boolean isPresent();

    public abstract boolean isSystem();

    public abstract int getType();

    public abstract int getSelector();

    public abstract int getLimit();

    public abstract int getBase();

    public abstract boolean getDefaultSizeFlag();

    public abstract int getRPL();

    public abstract void setRPL(int cpl);

    public abstract int getDPL();

    public abstract boolean setSelector(int selector);

    public abstract void checkAddress(int offset) throws ProcessorException;

    public abstract int translateAddressRead(int offset);

    public abstract int translateAddressWrite(int offset);

    public abstract void printState();

    public byte getByte(int offset)
    {
        return memory.getByte(translateAddressRead(offset));
    }

    public short getWord(int offset)
    {
        return memory.getWord(translateAddressRead(offset));
    }

    public int getDoubleWord(int offset)
    {
        return memory.getDoubleWord(translateAddressRead(offset));
    }

    public long getQuadWord(int offset)
    {
        int off = translateAddressRead(offset);
        long result = 0xFFFFFFFFl & memory.getDoubleWord(off);
        off = translateAddressRead(offset + 4);
        result |= (((long) memory.getDoubleWord(off)) << 32);
        return result;
    }

    public void setByte(int offset, byte data)
    {
        memory.setByte(translateAddressWrite(offset), data);
    }

    public void setWord(int offset, short data)
    {
        memory.setWord(translateAddressWrite(offset), data);
    }

    public void setDoubleWord(int offset, int data)
    {
        memory.setDoubleWord(translateAddressWrite(offset), data);
    }

    public void setQuadWord(int offset, long data)
    {
        int off = translateAddressWrite(offset);
        memory.setDoubleWord(off, (int) data);
        off = translateAddressWrite(offset + 4);
        memory.setDoubleWord(off, (int) (data >>> 32));
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": Segment:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        output.println("\tmemory <object #" + output.objectNumber(memory) + ">"); if(memory != null) memory.dumpStatus(output);
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        output.dumpObject(memory);
    }

    public Segment(SRLoader input) throws IOException
    {
        input.objectCreated(this);
        memory = (AddressSpace)input.loadObject();
    }
}
