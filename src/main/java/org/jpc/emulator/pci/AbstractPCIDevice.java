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

import org.jpc.emulator.*;
import java.io.*;

/**
 * Provides a default implementations for the core features of a standard PCI
 * device.  This includes assignment of device/function numbers, handling of
 * interrupts and implementation of the PCI configuration space for this device.
 * @author Chris Dennis
 */
public abstract class AbstractPCIDevice extends AbstractHardwareComponent implements PCIDevice
{
    private int deviceFunctionNumber;
    private byte[] configuration;
    private int irq;
    private IRQBouncer irqBouncer;
    private boolean pciRegistered;

    public void dumpStatusPartial(StatusDumper output)
    {
        super.dumpStatusPartial(output);
        output.println("\tdeviceFunctionNumber " + deviceFunctionNumber + " irq " + irq + " pciRegistered " + pciRegistered);
        output.println("\tirqBouncer <object #" + output.objectNumber(irqBouncer) + ">"); if(irqBouncer != null) irqBouncer.dumpStatus(output);
        output.println("\tconfiguration:");
        output.printArray(configuration, "configuration");
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": AbstractPCIDevice:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        super.dumpSRPartial(output);
        output.dumpInt(deviceFunctionNumber);
        output.dumpInt(irq);
        output.dumpBoolean(pciRegistered);
        output.dumpObject(irqBouncer);
        output.dumpArray(configuration);
    }

    public AbstractPCIDevice(SRLoader input) throws IOException
    {
        super(input);
        deviceFunctionNumber = input.loadInt();
        irq = input.loadInt();
        pciRegistered = input.loadBoolean();
        irqBouncer = (IRQBouncer)input.loadObject();
        configuration = input.loadArrayByte();
    }

    public AbstractPCIDevice()
    {
        pciRegistered = false;
        configuration = new byte[256];
    }

    //PCI Bus Registering

    public int getDeviceFunctionNumber()
    {
        return deviceFunctionNumber;
    }

    public void assignDeviceFunctionNumber(int devFN)
    {
        deviceFunctionNumber = devFN;
    }

    public boolean autoAssignDeviceFunctionNumber()
    {
        return true;
    }

    public void deassignDeviceFunctionNumber()
    {
        pciRegistered = false;
        assignDeviceFunctionNumber(-1);
    }

    private boolean checkConfigWrite(int address)
    {
        switch (0xff & configReadByte(PCI_CONFIG_HEADER)) {
            case PCI_HEADER_SINGLE_FUNCTION:
            case PCI_HEADER_MULTI_FUNCTION:
                switch (address) {
                    case PCI_CONFIG_VENDOR_ID:
                    case PCI_CONFIG_VENDOR_ID + 1:
                    case PCI_CONFIG_DEVICE_ID:
                    case PCI_CONFIG_DEVICE_ID + 1:

                    case PCI_CONFIG_REVISION:
                    case PCI_CONFIG_REVISION + 1:
                    case PCI_CONFIG_CLASS_DEVICE:
                    case PCI_CONFIG_CLASS_DEVICE + 1:

                    case PCI_CONFIG_HEADER:

                    case PCI_CONFIG_BASE_ADDRESS:
                    case PCI_CONFIG_BASE_ADDRESS + 0x01:
                    case PCI_CONFIG_BASE_ADDRESS + 0x02:
                    case PCI_CONFIG_BASE_ADDRESS + 0x03:
                    case PCI_CONFIG_BASE_ADDRESS + 0x04:
                    case PCI_CONFIG_BASE_ADDRESS + 0x05:
                    case PCI_CONFIG_BASE_ADDRESS + 0x06:
                    case PCI_CONFIG_BASE_ADDRESS + 0x07:
                    case PCI_CONFIG_BASE_ADDRESS + 0x08:
                    case PCI_CONFIG_BASE_ADDRESS + 0x09:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0a:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0b:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0c:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0d:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0e:
                    case PCI_CONFIG_BASE_ADDRESS + 0x0f:
                    case PCI_CONFIG_BASE_ADDRESS + 0x10:
                    case PCI_CONFIG_BASE_ADDRESS + 0x11:
                    case PCI_CONFIG_BASE_ADDRESS + 0x12:
                    case PCI_CONFIG_BASE_ADDRESS + 0x13:
                    case PCI_CONFIG_BASE_ADDRESS + 0x14:
                    case PCI_CONFIG_BASE_ADDRESS + 0x15:
                    case PCI_CONFIG_BASE_ADDRESS + 0x16:
                    case PCI_CONFIG_BASE_ADDRESS + 0x17:

                    case PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS:
                    case PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS + 0x1:
                    case PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS + 0x2:
                    case PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS + 0x3:

                    case PCI_CONFIG_INTERRUPT_PIN:
                        return false;

                    default:
                        return true;
                }
            default:
            case PCI_HEADER_PCI_PCI_BRIDGE:
                switch (address) {
                    case PCI_CONFIG_VENDOR_ID:
                    case PCI_CONFIG_VENDOR_ID + 1:
                    case PCI_CONFIG_DEVICE_ID:
                    case PCI_CONFIG_DEVICE_ID + 1:

                    case PCI_CONFIG_REVISION:
                    case PCI_CONFIG_REVISION + 1:
                    case PCI_CONFIG_CLASS_DEVICE:
                    case PCI_CONFIG_CLASS_DEVICE + 1:

                    case PCI_CONFIG_HEADER:

                    case 0x38: //RESERVED
                    case 0x39: //RESERVED
                    case 0x3a: //RESERVED
                    case 0x3b: //RESERVED

                    case PCI_CONFIG_INTERRUPT_PIN:
                        return false;

                    default:
                        return true;
                }
        }
    }

    public final boolean configWriteByte(int address, byte data) //returns true if device needs remapping

    {
        if (checkConfigWrite(address))
            putConfigByte(address, data);

        if (address >= PCI_CONFIG_COMMAND && address < (PCI_CONFIG_COMMAND + 2))
            /* if the command register is modified, we must modify the mappings */
            return true;
        return false;
    }

    public final boolean configWriteWord(int address, short data) //returns true if device needs remapping

    {
        int modAddress = address;
        for (int i = 0; i < 2; i++) {
            if (checkConfigWrite(modAddress))
                putConfigByte(modAddress, (byte) data);
            modAddress++;
            data >>>= 8;
        }

        if ((modAddress > PCI_CONFIG_COMMAND) && (address < (PCI_CONFIG_COMMAND + 2)))
            // if the command register is modified, we must modify the mappings
            return true;
        return false;
    }

    public final boolean configWriteLong(int address, int data)
    {
        if (((address >= PCI_CONFIG_BASE_ADDRESS && address < (PCI_CONFIG_BASE_ADDRESS + 4 * 6)) || (address >= PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS && address < (PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS + 4)))) {
            int regionIndex;
            if (address >= PCI_CONFIG_EXPANSION_ROM_BASE_ADDRESS)
                regionIndex = PCI_ROM_SLOT;
            else
                regionIndex = (address - PCI_CONFIG_BASE_ADDRESS) >>> 2;
            IORegion r = getIORegion(regionIndex);

            if (r != null) {
                if (regionIndex == PCI_ROM_SLOT)
                    data &= (~(r.getSize() - 1)) | 1;
                else {
                    data &= ~(r.getSize() - 1);
                    data |= r.getType();
                }
                putConfigLong(address, data);
                return true;
            }
        }

        int modAddress = address;
        for (int i = 0; i < 4; i++) {
            if (checkConfigWrite(modAddress))
                putConfigByte(modAddress, (byte) data);
            modAddress++;
            data = data >>> 8;
        }

        if (modAddress > PCI_CONFIG_COMMAND && address < (PCI_CONFIG_COMMAND + 2))
            /* if the command register is modified, we must modify the mappings */
            return true;
        return false;
    }

    public final byte configReadByte(int address)
    {
        return configuration[address];
    }

    public final short configReadWord(int address)
    {
        short result = configReadByte(address + 1);
        result <<= 8;
        result |= (0xff & configReadByte(address));
        return result;
    }

    public final int configReadLong(int address)
    {
        int result = 0xffff & configReadWord(address + 2);
        result <<= 16;
        result |= (0xffff & configReadWord(address));
        return result;
    }

    public final void putConfigByte(int address, byte data)
    {
        configuration[address] = data;
    }

    public final void putConfigWord(int address, short data)
    {
        putConfigByte(address, (byte) data);
        address++;
        data >>= 8;
        putConfigByte(address, (byte) data);
    }

    public final void putConfigLong(int address, int data)
    {
        putConfigWord(address, (short) data);
        address += 2;
        data >>= 16;
        putConfigWord(address, (short) data);
    }

    public void setIRQIndex(int irqIndex)
    {
        irq = irqIndex;
    }

    public int getIRQIndex()
    {
        return irq;
    }

    public void addIRQBouncer(IRQBouncer bouncer)
    {
        irqBouncer = bouncer;
    }

    public IRQBouncer getIRQBouncer()
    {
        return irqBouncer;
    }

    public boolean initialised()
    {
        return pciRegistered;
    }

    public void reset()
    {
        pciRegistered = false;
    }

    public void acceptComponent(HardwareComponent component)
    {
        if ((component instanceof PCIBus) && component.initialised() && !pciRegistered)
            pciRegistered = ((PCIBus) component).registerDevice(this);
    }

    public boolean wantsMappingUpdate()
    {
        return false;
    }
}
