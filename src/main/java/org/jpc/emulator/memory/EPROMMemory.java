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

package org.jpc.emulator.memory;
import org.jpc.emulator.StatusDumper;
import org.jpc.emulator.SRLoader;
import org.jpc.emulator.SRDumper;
import java.io.*;

import org.jpc.emulator.memory.codeblock.CodeBlockManager;

/**
 * Provides a read-only memory implementation in which the contents of ROM chips
 * can be stored (for example System and VGA bioses).
 * <p>
 * Attempts to perform any kind of write on an <code>EPROMMemory</code> will
 * silently fail.
 * @author Chris Dennis
 */
public class EPROMMemory extends LazyCodeBlockMemory
{
    /**
     * Constructs an instance with contents equal to a
     * fragment of the supplied array.
     * @param data source for this objects data.
     * @param offset index into <code>data</code> array.
     * @param length number of bytes copied into object.
     */
    public EPROMMemory(byte[] data, int offset, int length, CodeBlockManager manager)
    {
        this(length, 0, data, offset, length, manager);
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        super.dumpStatusPartial(output);
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": EPROMMemory:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        super.dumpSRPartial(output);
    }

    public EPROMMemory(SRLoader input) throws IOException
    {
        super(input);
    }

    /**
     * Constructs a <code>size</code> byte long instance with partial contents
     * copied from <code>data</code>.
     * @param size length of the instance.
     * @param base start index to copy data to.
     * @param data array to copy data from.
     * @param offset offset in array to copy data from.
     * @param length number of bytes to copy.
     */
    public EPROMMemory(int size, int base, byte[] data, int offset, int length, CodeBlockManager manager)
    {
        super(size, manager);
        super.copyArrayIntoContents(base, data, offset, Math.min(size - base, Math.min(length, data.length - offset)));
    }

    /**
     * Silently returns as this is a read-only instance.
     */
    public void setByte(int offset, byte data)
    {
        //writeAttempted(offset, 1);
    }

    /**
     * Silently returns as this is a read-only instance.
     */
    public void setWord(int offset, short data)
    {
        //writeAttempted(offset, 2);
    }

    /**
     * Silently returns as this is a read-only instance.
     */
    public void setDoubleWord(int offset, int data)
    {
        writeAttempted(offset, 4);
    }

    /**
     * Silently returns as this is a read-only instance.
     */
    public void copyArrayIntoContents(int address, byte[] buf, int off, int len)
    {
        writeAttempted(address, len);
    }

    public void clear()
    {
        constructCodeBlocksArray();
    }

    public String toString()
    {
        return "EPROM Memory [" + getSize() + "]";
    }

    private void writeAttempted(int address, int size)
    {
        if(size == 1)
            System.err.println("Warning: Write of 1 byte attempted at address " + Integer.toHexString(address) + ".");
        else
            System.err.println("Warning: Write of " + size + " bytes attempted at address " +
                Integer.toHexString(address) + ".");
    }
}
