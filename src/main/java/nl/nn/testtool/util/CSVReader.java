/*
 * CSVReader.java can be used to construct CSV's from a reader.
 *
 * Copyright (C) 1999 Jaco de Groot (jaco@dynasol.nl)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

// Slightly changed version of nl.dynasol.csv.CSVReader (use List instead of Vector)
package nl.nn.testtool.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class considers lines to be terminated by the line feed ('\n')
 * character. You may want to use the LineNumberReader class to translate
 * carriage return ('\r') or a carriage return followed immediately by a
 * line feed to a single line feed.
 *
 * <br>
 * <br>
 * 
 * You could for example use the following code to read csv files:
 * <pre>
 * FileReader fr = new FileReader(file);
 * LineNumberReader lnr = new LineNumberReader(fr);
 * CSVReader cr = new CSVReader(lnr);
 * Vector v;
 * while (cr.hasMoreElements()) {
 *     v = cr.nextCSV();
 *     ...
 * }
 * cr.close();
 * </pre>
 *
 * @author  Jaco de Groot
 */
public class CSVReader {
    Reader r;
    char separator = ',';
    int state;
    int lineNumberLast = -1;
    int lineNumberNext = 1;
    
    public CSVReader(Reader r) {
        this.r = r;
        state = 0;
    }

    public void setSeparator(char c) {
        separator = c;
    }

    public boolean hasMoreElements() {
        boolean hasMoreElements;
        if (state == 4) {
            hasMoreElements = false;
        } else {
            hasMoreElements = true;
        }
        return hasMoreElements;
    }

/*

   +--------------+-----------------------------+
   |              |                             |
   +-->(0)--','---+                             |
        |         |                             |
        +--'\n'---+                             |
        |                                       |
        |         +-------------'"'---+---','---+
        |         |                   |         |
        +---'"'---+------->(1)--'"'->(2)-'\n'---+
        |         |         |         |         |
        |         +---rem---+         |         |
        |                             |         |
        +--'-1'->(4)<-----------'-1'--+         |
        |                             |         |
        +---rem-------------+------->(3)--','---+
                            |         |         |
                            +---rem---+--'\n'---+

Def: rem is alle characters behalve waar al een pijl voor is.
Opm: rem bevat nooit -1.
Een komma is scheidingsteken voor csv-velden en newline is scheidingsteken
voor csv's. Dus op einde stream verwacht je geen newline tenzij er natuurlijk
een csv met 1 leeg veld op het einde zit. Met deze regel kun je CSVReader ook
gebruiken om een string m.b.v. stringreader om te zetten naar csv. Want bij een
string verwacht je op het einde ook geen newline tenzij...
*/

    public List nextCSV() throws NoSuchElementException, IOException {
        if (state == 4) {
            throw new NoSuchElementException();
        }
        lineNumberLast = lineNumberNext;
        int c;
        List l = new ArrayList();
        StringBuffer sb = new StringBuffer();
        while (true) {
            c = r.read();
            if (state == 0) {
                if (c == separator) {
                    l.add(null);
                } else if (c == '\n') {
                    l.add(null);
                    lineNumberNext++;
                    return l;
                } else if (c == '"') {
                    state = 1;
                } else if (c == -1) {
                    state = 4;
                    l.add(null);
                    return l;
                } else {
                    state = 3;
                    sb.append((char) c);
                }
            } else if (state == 1) {
                if (c == '"') {
                    state = 2;
                } else if (c == -1) {
                    // nog doen:
                    // throw new IllegalEndOfStreamException(); strikte modus maken? en alleen in strikte modes doen?
                    l.add(sb.toString());
                    return l;
                } else {
                    if (c == '\n') {
                      lineNumberNext++;
                    }
                    sb.append((char) c);
                }
            } else if (state == 2) {
                if (c == '"') {
                    state = 1;
                    sb.append('"');
                } else if (c == separator) {
                    state = 0;
                    l.add(sb.toString());
                    sb = new StringBuffer();
                } else if (c == '\n') {
                    state = 0;
                    l.add(sb.toString());
                    lineNumberNext++;
                    return l;
                } else if (c == -1) {
                    state = 4;
                    l.add(sb.toString());
                    return l;
                } else {
                    // nog doen:
                    // throw IllegalCharacterException;  strikte modus maken? en alleen in strikte modes doen?
                }
            } else { // state == 3
                if (c == separator) {
                    state = 0;
                    l.add(sb.toString());
                    sb = new StringBuffer();
                } else if (c == '\n') {
                    state = 0;
                    l.add(sb.toString());
                    lineNumberNext++;
                    return l;
                } else if (c == -1) {
                    state = 4;
                    l.add(sb.toString());
                    return l;
                } else {
                    sb.append((char) c);
                }
            }
        }
    }

    /**
     * Get first line number of the last CSV record.
     */
    public int getLineNumberLast() {
        return lineNumberLast;
    }

    /**
     * Get first line number of the next CSV record.
     */
    public int getLineNumberNext() {
        return lineNumberNext;
    }

    public void close() throws IOException {
        r.close();
    }

}
