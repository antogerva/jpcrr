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

package org.jpc.emulator.memory.codeblock;

import java.io.*;

import org.jpc.emulator.SRLoader;
import org.jpc.emulator.SRDumper;
import org.jpc.emulator.SRDumpable;
import org.jpc.emulator.memory.Memory;
import org.jpc.emulator.memory.codeblock.optimised.*;

/**
 * Provides the outer skin for the codeblock construction system.
 * <p>
 * If blocks are not found in memory, then they are requested from this class.
 * @author Chris Dennis
 */
public class CodeBlockManager implements SRDumpable
{
    public static volatile int BLOCK_LIMIT = 1000; //minimum of 2 because of STI/CLI
    private CodeBlockFactory realModeChain,  protectedModeChain,  virtual8086ModeChain;
    private ByteSourceWrappedMemory byteSource;

    /**
     * Constructs a default manager.
     * <p>
     * The default manager creates interpreted mode codeblocks.
     */
    public CodeBlockManager()
    {
        byteSource = new ByteSourceWrappedMemory();

        realModeChain = new DefaultCodeBlockFactory(new RealModeUDecoder(), new OptimisedCompiler(), BLOCK_LIMIT);
        protectedModeChain = new DefaultCodeBlockFactory(new ProtectedModeUDecoder(), new OptimisedCompiler(), BLOCK_LIMIT);
        virtual8086ModeChain = new DefaultCodeBlockFactory(new RealModeUDecoder(), new OptimisedCompiler(), BLOCK_LIMIT);
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
    }

    public CodeBlockManager(SRLoader input)
    {
        this();
        input.objectCreated(this);
    }

    private RealModeCodeBlock tryRealModeFactory(CodeBlockFactory ff, Memory memory, int offset)
    {
        try {
            byteSource.set(memory, offset);
            return ff.getRealModeCodeBlock(byteSource);
        } catch (ArrayIndexOutOfBoundsException e) {
            return new SpanningRealModeCodeBlock(new CodeBlockFactory[]{realModeChain});
        }
    }

    private ProtectedModeCodeBlock tryProtectedModeFactory(CodeBlockFactory ff, Memory memory, int offset, boolean operandSizeFlag)
    {
        try {
            byteSource.set(memory, offset);
            return ff.getProtectedModeCodeBlock(byteSource, operandSizeFlag);
        } catch (ArrayIndexOutOfBoundsException e) {
            return new SpanningProtectedModeCodeBlock(new CodeBlockFactory[]{protectedModeChain});
        }
    }

    private Virtual8086ModeCodeBlock tryVirtual8086ModeFactory(CodeBlockFactory ff, Memory memory, int offset)
    {
        try {
            byteSource.set(memory, offset);
            return ff.getVirtual8086ModeCodeBlock(byteSource);
        } catch (ArrayIndexOutOfBoundsException e) {
            return new SpanningVirtual8086ModeCodeBlock(new CodeBlockFactory[]{virtual8086ModeChain});
        }
    }

    /**
     * Get a real mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @return real mode codeblock instance
     */
    public RealModeCodeBlock getRealModeCodeBlockAt(Memory memory, int offset)
    {
        RealModeCodeBlock block;

        if((block = tryRealModeFactory(realModeChain, memory, offset)) == null) {
            System.err.println("Critical error: Can't find nor make suitable real mode codeblock.");
            throw new IllegalStateException("Couldn't find/make suitable realmode block");
        }
        return block;

    }

    /**
     * Get a protected mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @param operandSize <code>true</code> for 32-bit, <code>false</code> for 16-bit
     * @return protected mode codeblock instance
     */
    public ProtectedModeCodeBlock getProtectedModeCodeBlockAt(Memory memory, int offset, boolean operandSize)
    {
        ProtectedModeCodeBlock block;

        if((block = tryProtectedModeFactory(protectedModeChain, memory, offset, operandSize)) == null) {
            System.err.println("Critical error: Can't find nor make suitable protected mode codeblock.");
            throw new IllegalStateException("Couldn't find/make suitable pmode block");
        }
        return block;
    }

    /**
     * Get a Virtual8086 mode codeblock instance for the given memory area.
     * @param memory source for the x86 bytes
     * @param offset address in the given memory object
     * @return Virtual8086 mode codeblock instance
     */
    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlockAt(Memory memory, int offset)
    {
        Virtual8086ModeCodeBlock block;

        if((block = tryVirtual8086ModeFactory(virtual8086ModeChain, memory, offset)) == null) {
            System.err.println("Critical error: Can't find nor make suitable VM8086 mode codeblock.");
            throw new IllegalStateException("Couldn't find/make suitable VM86 block");
        }
        return block;
    }
}
