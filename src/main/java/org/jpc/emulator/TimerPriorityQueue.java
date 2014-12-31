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

package org.jpc.emulator;

import java.io.*;

//The reason this exists is that standard Java PriorityQueue breaks ties in arbitiary way. This
//application requires that ties are broken deterministically. In this case, the policy is first-
//in-first-out.


/**
 *
 * @author Ilari Liusvaara
 */
public class TimerPriorityQueue implements SRDumpable
{
    private Node first, last;

    public static class Node
    {
        public Timer timer;
        public Node next;
    }

    public void dumpSRPartial(SRDumper output) throws IOException
    {
        Node current = first;
        while(current != null) {
            output.dumpBoolean(true);
            output.dumpObject(current.timer);
            current = current.next;
        }
        output.dumpBoolean(false);
    }

    public TimerPriorityQueue(SRLoader input) throws IOException
    {
        input.objectCreated(this);
        boolean present = input.loadBoolean();
        first = null;
        while(present) {
            if(last != null)
                last = last.next = new Node();
            else
                last = first = new Node();
            last.timer = (Timer)input.loadObject();
            present = input.loadBoolean();
        }
    }

    public TimerPriorityQueue()
    {
    }

    public void dumpStatusPartial(StatusDumper output)
    {
        //super.dumpStatusPartial(output); <no superclass 20090704>
        Node current = first;
        while(current != null) {
            output.println("\ttimernode <object #" + output.objectNumber(current.timer) + ">"); if(current.timer != null) current.timer.dumpStatus(output);
            current = current.next;
        }
    }

    public void dumpStatus(StatusDumper output)
    {
        if(output.dumped(this))
            return;

        output.println("#" + output.objectNumber(this) + ": TimerPriorityQueue:");
        dumpStatusPartial(output);
        output.endObject();
    }

    public Timer peek()
    {
        if(first != null)
            return first.timer;
        else
            return null;
    }

    public void remove(Timer t)
    {
        Node previousNode = null;
        Node currentNode = first;

        while(currentNode != null) {
            if(currentNode.timer == t) {
                if(previousNode == null)
                    first = currentNode.next;
                else
                    previousNode.next = currentNode.next;
                if(currentNode == last)
                    last = previousNode;
                return;
            }
            previousNode = currentNode;
            currentNode = currentNode.next;
        }
    }

    public void offer(Timer t)
    {
        Node newNode = new Node();
        newNode.timer = t;
        Node previousNode = null;
        Node currentNode = first;

        while(currentNode != null) {
            if(t.compareTo(currentNode.timer) < 0) {
                //This is the first node that's later than node to be added. Insert
                //between previousNode and currentNode.
                newNode.next = currentNode;
                if(previousNode == null)
                    first = newNode;
                else
                    previousNode.next = newNode;
                return;
            }
            previousNode = currentNode;
            currentNode = currentNode.next;
         }
        //All existing timers should go first.
        if(previousNode != null)
            previousNode.next = newNode;
        else
            first = newNode;
        last = newNode;
    }

    public String toString()
    {
        return "Timer Event Queue";
    }
}
