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

package org.jpc.emulator.pci;
import org.jpc.emulator.StatusDumper;
import org.jpc.emulator.SRDumpable;

/**
 * A object that allows a PCI device to raise an interrupt on the processor.
 * <p>
 * Instances of this class are handed out to PCI devices by the PCI-ISA bridge
 * so that interrupt request can be directed straight to the ISA bridge, and
 * therefore removing the indirection of access through the PCI bus itself.
 * @author Chris Dennis
 */
public interface IRQBouncer extends SRDumpable
{
    /**
     * Raise or lower the given interrupt on the processor.
     * @param device source of the request
     * @param irqNumber interrupt number to adjust
     * @param level 1 to raise, 0 to lower.
     */
    public void setIRQ(PCIDevice device, int irqNumber, int level);

    public void dumpStatus(StatusDumper output);
}
