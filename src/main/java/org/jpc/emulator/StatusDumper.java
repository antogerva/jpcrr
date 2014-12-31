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

package org.jpc.emulator;

import java.io.*;

public class StatusDumper
{
    int extraIndent;
    java.util.HashMap<String, Integer> nextObjectNumber;
    PrintStream underlyingPrintStream;
    static final Boolean TRUE;
    static final Boolean FALSE;
    java.util.HashMap<String, Boolean> seenObjects;
    java.util.HashMap<Integer, ObjectListEntry> chainingLists;
    int objectsCount;

    static class ObjectListEntry
    {
        public Object object;
        public String num;
        public ObjectListEntry next;
    }

    static
    {
        TRUE = new Boolean(true);
        FALSE = new Boolean(false);
    }

    public StatusDumper(PrintStream ps)
    {
        extraIndent = -1;
        nextObjectNumber = new java.util.HashMap<String, Integer>();
        underlyingPrintStream = ps;
        seenObjects = new java.util.HashMap<String, Boolean>();
        chainingLists = new java.util.HashMap<Integer, ObjectListEntry>();
        objectsCount = 0;
    }

    public int dumpedObjects()
    {
        return objectsCount;
    }

    public void println(String S)
    {
        String X = "";
        for(int i = 0; i < extraIndent; i++)
            X = X + "\t";
        X = X + S;
        underlyingPrintStream.println(X);
    }

    public void printArray(boolean[] A, String N)
    {
        if(A == null) {
            println("\t" + N + " null");
            return;
        }
        println("\t" + N  + ":");
        String S = "\t\t";
        for(int i = 0; i < A.length; i++) {
            if(i % 16 == 15) {
                S = S + A[i] + "";
                println(S);
                S = "\t\t";
            } else {
                S = S + A[i] + " ";
            }
        }
    }

    public void printArray(byte[] A, String N)
    {
        if(A == null) {
            println("\t" + N + " null");
            return;
        }
        println("\t" + N  + ":");
        String S = "\t\t";
        for(int i = 0; i < A.length; i++) {
            if(i % 16 == 15) {
                S = S + A[i] + "";
                println(S);
                S = "\t\t";
            } else {
                S = S + A[i] + " ";
            }
        }
    }

    public void printArray(int[] A, String N)
    {
        if(A == null) {
            println("\t" + N + " null");
            return;
        }
        println("\t" + N + ":");
        String S = "\t\t";
        for(int i = 0; i < A.length; i++) {
            if(i % 16 == 15) {
                S = S + A[i] + "";
                println(S);
                S = "\t\t";
            } else {
                S = S + A[i] + " ";
            }
        }
    }

    public void printArray(long[] A, String N)
    {
        if(A == null) {
            println("\t" + N + " null");
            return;
        }
        println("\t" + N + ":");
        String S = "\t\t";
        for(int i = 0; i < A.length; i++) {
            if(i % 16 == 15) {
                S = S + A[i] + "";
                println(S);
                S = "\t\t";
            } else {
                S = S + A[i] + " ";
            }
        }
    }

    private void addObject(Object O, String n)
    {
        Integer hcode = new Integer(O.hashCode());
        ObjectListEntry e = new ObjectListEntry();
        e.object = O;
        e.num = n;
        e.next = null;
        if(!chainingLists.containsKey(hcode)) {
            chainingLists.put(hcode, e);
        } else {
            e.next = chainingLists.get(hcode);
            chainingLists.put(hcode, e);
        }
    }

    private String lookupObject(Object O)
    {
        Integer hcode = new Integer(O.hashCode());
        if(!chainingLists.containsKey(hcode))
            return null;
        ObjectListEntry e = chainingLists.get(hcode);
        while(e != null) {
            if(e.object == O)
                return e.num;
            e = e.next;
        }
        return null;
    }

    public String objectNumber(Object O)
    {
        String assignedNum;
        boolean isNew = false;

        if(O == null)
            return "NULL";

        assignedNum = lookupObject(O);
        if(assignedNum == null)
            isNew = true;

        if(isNew) {
            String cName = O.getClass().getName();
            if(!nextObjectNumber.containsKey(cName)) {
                nextObjectNumber.put(cName, new Integer(1));
                assignedNum = cName + "-1";
            } else {
                int seqno = nextObjectNumber.get(cName).intValue();
                nextObjectNumber.put(cName, new Integer(seqno + 1));
                assignedNum = cName + "-" + (seqno + 1);
            }
            addObject(O, assignedNum);
            seenObjects.put(assignedNum, FALSE);
        }
        return assignedNum;
    }

    public boolean dumped(Object O)
    {
        boolean seenBefore = false;
        String obj = objectNumber(O);

        seenBefore = seenObjects.get(obj).booleanValue();
        if(!seenBefore) {
            extraIndent++;
            seenObjects.put(obj, TRUE);
            objectsCount++;
            return false;
        } else
            return true;
    }

    public void endObject()
    {
        println("--- END OF OBJECT ---");
        extraIndent--;
    }
}
