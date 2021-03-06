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
 * A region provided by a PCI device that can at runtime be mapped into one of
 * the emulated machines address spaces.
 * @author Chris Dennis
 */
public interface IORegion extends SRDumpable
{
    public static final int PCI_ADDRESS_SPACE_MEM = 0x00;
    public static final int PCI_ADDRESS_SPACE_IO = 0x01;
    public static final int PCI_ADDRESS_SPACE_MEM_PREFETCH = 0x08;

    /**
     * Returns the starting address of the area that this region is mapped to.
     * @return starting address of this region.
     */
    public int getAddress();

    /**
     * Returns the length of this region in bytes.
     * @return size in bytes.
     */
    public long getSize();

    /**
     * Returns an integer representing the type of this region.
     * @return integer type.
     */
    public int getType();

    /**
     * Returns the region number or index of this region.
     * <p>
     * In any given PCI device, <code>IORegion</code>s are not required to be
     * contiguous.
     * @return region number.
     */
    public int getRegionNumber();

    /**
     * Tells this region that it has been mapped into it's associated address
     * space at the given address.
     * @param address start address of the mapping.
     */
    public void setAddress(int address);

    public void dumpStatus(StatusDumper output);
}
