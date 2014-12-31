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

package org.jpc.emulator.memory.codeblock;

/**
 * Converts a stream of JPC instructions into an executable codeblock.
 * @author Chris Dennis
 */
public interface CodeBlockCompiler
{
    /**
     * Create a real-mode codeblock from the given instruction source.
     * @param source instructions read from here
     * @return codeblock instance
     */
    public RealModeCodeBlock getRealModeCodeBlock(InstructionSource source);

    /**
     * Create a protected-mode codeblock from the given instruction source.
     * @param source instructions read from here
     * @return codeblock instance
     */
    public ProtectedModeCodeBlock getProtectedModeCodeBlock(InstructionSource source);

    /**
     * Create a virtual8086-mode codeblock from the given instruction source.
     * @param source instructions read from here
     * @return codeblock instance
     */
    public Virtual8086ModeCodeBlock getVirtual8086ModeCodeBlock(InstructionSource source);
}
