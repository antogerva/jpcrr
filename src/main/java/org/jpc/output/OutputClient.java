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

package org.jpc.output;

import java.io.*;
import java.util.*;

public class OutputClient
{
    OutputStatic server;
    int state;

    public OutputClient(OutputStatic _serv)
    {
        state = 0;
        server = _serv;
        server.clientNew(this);
    }

    public void detach()
    {
        server.clientDestroy(this);
        server = null;
    }

    public boolean aquire()
    {
        return server.clientAquire(this);
    }

    public void release()
    {
        server.clientRelease(this, false);
    }

    public void releaseWaitAll()
    {
        server.clientRelease(this, true);
    }

    protected void setState(int s)
    {
        state = s;
    }

    protected int getState()
    {
        return state;
    }
};
