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
import org.jpc.emulator.motherboard.*;
import org.jpc.emulator.HardwareComponent;
import org.jpc.emulator.SRLoader;
import org.jpc.emulator.SRDumper;

import java.io.*;

/**
 * Emulation of an Intel i440FX PCI Host Bridge.
 * <p>
 * The host bridge is the PCI device that provides the processor with access to
 * the PCI bus and the rest if its devices.
 * @author Chris Dennis
 */
public class PCIHostBridge extends AbstractPCIDevice implements IOPortCapable
{
    private PCIBus attachedBus;

    private int configRegister;

    public void dumpStatusPartial(StatusDumper output)
    {
        super.dumpStatusPartial(output);
        output.println("\tconfigRegister" + configRegister);
        output.println("\tattachedBus <object #" + output.objectNumber(attachedBus) + ">"); if(attachedBus != null) attachedBus.dumpStatus(output);
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": PCIHostBridge:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        super.dumpSRPartial(output);
        output.dumpInt(configRegister);
        output.dumpObject(attachedBus);
    }

    public PCIHostBridge(SRLoader input) throws IOException
    {
        super(input);
        configRegister = input.loadInt();
        attachedBus = (PCIBus)input.loadObject();
    }

    /**
     * Constructs the (singleton) host bridge for a pci bus.
     */
    public PCIHostBridge()
    {
        ioportRegistered = false;

        assignDeviceFunctionNumber(0);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // vendor_id
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x1237); // device_id
        putConfigByte(PCI_CONFIG_REVISION, (byte)0x02); // revision
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0600); // pci host bridge
        putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type
    }

    public boolean autoAssignDeviceFunctionNumber()
    {
        return false;
    }

    public void deassignDeviceFunctionNumber()
    {
        System.err.println("Warning: PCI device/function number conflict.");
    }

    /* BEGIN PCIDevice Methods */
    //IOPort Registration Aids
    public IORegion[] getIORegions()
    {
        return null;
    }
    public IORegion getIORegion(int index)
    {
        return null;
    }

    public int[] ioPortsRequested()
    {
        return new int[]{0xcf8, 0xcf9, 0xcfa, 0xcfb, 0xcfc, 0xcfd, 0xcfe, 0xcff};
    }

    public void ioPortWriteByte(int address, int data)
    {
        switch (address) {
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) != 0)
                attachedBus.writePCIDataByte(configRegister | (address & 0x3), (byte)data);
            break;
        default:
        }
    }

    public void ioPortWriteWord(int address, int data)
    {
        switch(address) {
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) != 0)
                attachedBus.writePCIDataWord(configRegister | (address & 0x3), (short)data);
            break;
        default:
        }
    }

    public void ioPortWriteLong(int address, int data)
    {
        switch(address) {
        case 0xcf8:
        case 0xcf9:
        case 0xcfa:
        case 0xcfb:
            configRegister = data;
            break;
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) != 0)
                attachedBus.writePCIDataLong(configRegister | (address & 0x3), data);
            break;
        default:
        }
    }

    public int ioPortReadByte(int address)
    {
        switch(address) {
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) == 0)
                return 0xff;
            else
                return 0xff & attachedBus.readPCIDataByte(configRegister | (address & 0x3));

        default:
            return 0xff;
        }
    }

    public int ioPortReadWord(int address)
    {
        switch(address) {
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) == 0)
                return 0xffff;
            else
                return 0xffff & attachedBus.readPCIDataWord(configRegister | (address & 0x3));
        default:
            return 0xffff;
        }
    }

    public int ioPortReadLong(int address)
    {
        switch(address) {
        case 0xcf8:
        case 0xcf9:
        case 0xcfa:
        case 0xcfb:
            return configRegister;
        case 0xcfc:
        case 0xcfd:
        case 0xcfe:
        case 0xcff:
            if ((configRegister & (1 << 31)) == 0)
                return 0xffffffff;
            else
                return attachedBus.readPCIDataLong(configRegister | (address & 0x3));
        default:
            return 0xffffffff;
        }
    }

    /* END IOPortCapable Methods */

    private boolean ioportRegistered;
    private boolean pciRegistered;

    public boolean initialised()
    {
        return ioportRegistered && pciRegistered;
    }

    public void reset()
    {
        attachedBus = null;
        pciRegistered = false;
        ioportRegistered = false;

        assignDeviceFunctionNumber(0);

        putConfigWord(PCI_CONFIG_VENDOR_ID, (short)0x8086); // Intel
        putConfigWord(PCI_CONFIG_DEVICE_ID, (short)0x1237); // device_id
        putConfigByte(PCI_CONFIG_REVISION, (byte)0x02); // revision
        putConfigWord(PCI_CONFIG_CLASS_DEVICE, (short)0x0600); // pci host bridge
        putConfigByte(PCI_CONFIG_HEADER, (byte)0x00); // header_type
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof PCIBus) && component.initialised() && !pciRegistered) {
            attachedBus = (PCIBus)component;
            pciRegistered = attachedBus.registerDevice(this);
        }

        if ((component instanceof IOPortHandler)
            && component.initialised()) {
            ((IOPortHandler)component).registerIOPortCapable(this);
            ioportRegistered = true;
        }
    }

    public String toString()
    {
        return "Intel i440FX PCI-Host Bridge";
    }
}
