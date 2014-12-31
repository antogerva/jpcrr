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

package org.jpc.emulator.processor;

import org.jpc.emulator.memory.*;

import java.io.*;
import org.jpc.emulator.SRLoader;
import org.jpc.emulator.SRDumper;
import org.jpc.emulator.SRDumpable;
import org.jpc.emulator.StatusDumper;

/**
 *
 * @author Chris Dennis
 */
public class SegmentFactory implements SRDumpable
{
    private static final long DESCRIPTOR_TYPE = 0x100000000000L;
    private static final long SEGMENT_TYPE = 0xf0000000000L;

    public static final Segment NULL_SEGMENT = new NullSegment();

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": SegmentFactory:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        output.println("\tNULL_SEGMENT <object #" + output.objectNumber(NULL_SEGMENT) + ">"); if(NULL_SEGMENT != null) NULL_SEGMENT.dumpStatus(output);
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
    }

    public SegmentFactory(SRLoader input) throws IOException
    {
        input.objectCreated(this);
    }

    private SegmentFactory()
    {
    }

    protected SegmentFactory STFU()
    {
        SegmentFactory f = new SegmentFactory();
        return f;
    }

    public static Segment createRealModeSegment(AddressSpace memory, int selector)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new RealModeSegment(memory, selector);
    }

    public static Segment createRealModeSegment(AddressSpace memory, Segment ancestor)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new RealModeSegment(memory, ancestor);
    }

    public static Segment createVirtual8086ModeSegment(AddressSpace memory, int selector, boolean isCode)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new Virtual8086ModeSegment(memory, selector, isCode);
    }

    public static Segment createDescriptorTableSegment(AddressSpace memory, int base, int limit)
    {
        if (memory == null)
            throw new NullPointerException("Null reference to memory");

        return new DescriptorTableSegment(memory, base, limit);
    }

    public static Segment createProtectedModeSegment(AddressSpace memory, int selector, long descriptor)
    {
        switch ((int) ((descriptor & (DESCRIPTOR_TYPE | SEGMENT_TYPE)) >>> 40)) {

            // System Segments
            default:
            case 0x00: //Reserved
            case 0x08: //Reserved
            case 0x0a: //Reserved
            case 0x0d: //Reserved
                throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
            case 0x01: //System Segment: 16-bit TSS (Available)
                return new ProtectedModeSegment.Available16BitTSS(memory, selector, descriptor);
            case 0x02: //System Segment: LDT
                return new ProtectedModeSegment.LDT(memory, selector, descriptor);
            case 0x03: //System Segment: 16-bit TSS (Busy)
                return new ProtectedModeSegment.Busy16BitTSS(memory, selector, descriptor);
            case 0x04: //System Segment: 16-bit Call Gate
                return new ProtectedModeSegment.CallGate16Bit(memory, selector, descriptor);
            case 0x05: //System Segment: Task Gate
                return new ProtectedModeSegment.TaskGate(memory, selector, descriptor);
            case 0x06: //System Segment: 16-bit Interrupt Gate
                return new ProtectedModeSegment.InterruptGate16Bit(memory, selector, descriptor);
            case 0x07: //System Segment: 16-bit Trap Gate
                return new ProtectedModeSegment.TrapGate16Bit(memory, selector, descriptor);
            case 0x09: //System Segment: 32-bit TSS (Available)
                return new ProtectedModeSegment.Available32BitTSS(memory, selector, descriptor);
            case 0x0b: //System Segment: 32-bit TSS (Busy)
                return new ProtectedModeSegment.Busy32BitTSS(memory, selector, descriptor);
            case 0x0c: //System Segment: 32-bit Call Gate
                return new ProtectedModeSegment.CallGate32Bit(memory, selector, descriptor);
            case 0x0e: //System Segment: 32-bit Interrupt Gate
                return new ProtectedModeSegment.InterruptGate32Bit(memory, selector, descriptor);
            case 0x0f: //System Segment: 32-bit Trap Gate
                return new ProtectedModeSegment.TrapGate32Bit(memory, selector, descriptor);

            // Code and Data Segments
            case 0x10: //Data Segment: Read-Only
                return new ProtectedModeSegment.ReadOnlyDataSegment(memory, selector, descriptor);
            case 0x11: //Data Segment: Read-Only, Accessed
                return new ProtectedModeSegment.ReadOnlyAccessedDataSegment(memory, selector, descriptor);
            case 0x12: //Data Segment: Read/Write
                return new ProtectedModeSegment.ReadWriteDataSegment(memory, selector, descriptor);
            case 0x13: //Data Segment: Read/Write, Accessed
                return new ProtectedModeSegment.ReadWriteAccessedDataSegment(memory, selector, descriptor);
            case 0x14: //Data Segment: Read-Only, Expand-Down
                System.err.println("Critical error: Unimplemented Data Segment: Read-Only, Expand-Down");
                throw new IllegalStateException("Unimplemented Data Segment: Read-Only, Expand-Down");
            case 0x15: //Data Segment: Read-Only, Expand-Down, Accessed
                System.err.println("Critical error: Unimplemented Data Segment: Read-Only, Expand-Down, Accessed");
                throw new IllegalStateException("Unimplemented Data Segment: Read-Only, Expand-Down, Accessed");
            case 0x16: //Data Segment: Read/Write, Expand-Down
                return new ProtectedModeExpandDownSegment.ReadWriteDataSegment(memory, selector, descriptor);
            case 0x17: //Data Segment: Read/Write, Expand-Down, Accessed
                System.err.println("Critical error: Unimplemented Data Segment: Read/Write, Expand-Down, Accessed");
                throw new IllegalStateException("Unimplemented Data Segment: Read/Write, Expand-Down, Accessed");

            case 0x18: //Code, Execute-Only
                return new ProtectedModeSegment.ExecuteOnlyCodeSegment(memory, selector, descriptor);
            case 0x19: //Code, Execute-Only, Accessed
                System.err.println("Critical error: Unimplemented Code Segment: Execute-Only, Accessed");
                throw new IllegalStateException("Unimplemented Code Segment: Execute-Only, Accessed");
            case 0x1a: //Code, Execute/Read
                return new ProtectedModeSegment.ExecuteReadCodeSegment(memory, selector, descriptor);
            case 0x1b: //Code, Execute/Read, Accessed
                return new ProtectedModeSegment.ExecuteReadAccessedCodeSegment(memory, selector, descriptor);
            case 0x1c: //Code: Execute-Only, Conforming
                System.err.println("Critical error: Unimplemented Code Segment: Execute-Only, Conforming");
                throw new IllegalStateException("Unimplemented Code Segment: Execute-Only, Conforming");
            case 0x1d: //Code: Execute-Only, Conforming, Accessed
                return new ProtectedModeSegment.ExecuteOnlyConformingAccessedCodeSegment(memory, selector, descriptor);
            case 0x1e: //Code: Execute/Read, Conforming
                return new ProtectedModeSegment.ExecuteReadConformingCodeSegment(memory, selector, descriptor);
            case 0x1f: //Code: Execute/Read, Conforming, Accessed
                return new ProtectedModeSegment.ExecuteReadConformingAccessedCodeSegment(memory, selector, descriptor);
        }
    }

    public static final class NullSegment extends Segment
    {
        public void dumpStatusPartial(StatusDumper output)
        {
            super.dumpStatusPartial(output);
        }

        public void dumpStatus(StatusDumper output)
        {
            if(output.dumped(this))
                return;
            output.println("#" + output.objectNumber(this) + ": NullSegment:");
            dumpStatusPartial(output);
            output.endObject();
        }

        public void dumpSRPartial(SRDumper output) throws IOException
        {
            super.dumpSRPartial(output);
        }

        public NullSegment(SRLoader input) throws IOException
        {
            super(input);
        }


        public NullSegment()
        {
            super(null, true);
        }

        public void printState()
        {
            System.out.println("Null Segment");
        }

        public int getType()
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int getSelector()
        {
            return 0;
        }

        public void checkAddress(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int translateAddressRead(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int translateAddressWrite(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public void invalidateAddress(int offset)
        {
            throw new ProcessorException(ProcessorException.Type.GENERAL_PROTECTION, 0, true);//ProcessorException.GENERAL_PROTECTION_0;
        }

        public int getBase()
        {
            System.err.println("Critical error: NULL segment getBase()");
            throw new IllegalStateException("NULL segment getBase(): " + getClass().toString());
        }

        public int getLimit()
        {
            System.err.println("Critical error: NULL segment getLimit()");
            throw new IllegalStateException("NULL segment getLimit(): " + getClass().toString());
        }

        public int getRawLimit()
        {
            System.err.println("Critical error: NULL segment getRawLimit()");
            throw new IllegalStateException("NULL segment getRawLimit(): " + getClass().toString());
        }

        public boolean setSelector(int selector)
        {
            System.err.println("Critical error: NULL segment setSelector()");
            throw new IllegalStateException("NULL segment setSelector(): " + getClass().toString());
        }

        public int getDPL()
        {
            System.err.println("Critical error: NULL segment getDPL()");
            throw new IllegalStateException("NULL segment getDPL(): " + getClass().toString());
        }

        public int getRPL()
        {
            System.err.println("Critical error: NULL segment getRPL()");
            throw new IllegalStateException("NULL segment getRPL(): " + getClass().toString());
        }

        public void setRPL(int cpl)
        {
            System.err.println("Critical error: NULL segment setRPL()");
            throw new IllegalStateException("NULL segment setRPL(): " + getClass().toString());
        }

        public boolean getDefaultSizeFlag()
        {
            System.err.println("Critical error: NULL segment getDefaultSizeFlag()");
            throw new IllegalStateException("NULL segment getDefaultSizeFlag(): " + getClass().toString());
        }

        public boolean isPresent()
        {
            return true;
        }

        public boolean isSystem()
        {
            return false;
        }
    }
}
