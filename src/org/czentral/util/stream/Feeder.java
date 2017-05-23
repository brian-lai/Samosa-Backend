/*
    This file is part of "stream.m" software, a video broadcasting tool
    compatible with Google's WebM format.
    Copyright (C) 2011 Varga Bence

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.czentral.util.stream;


import java.io.*;

/**
 * A facility which feeds the data from an <code>InputStream</code> to a <code>Processor</code> until
 * the <code>Processor</code> finishes its work.
 * 
 * Since data is being red in chunks, in most cases a surplus of data is red. This <i>unprocessed<i> data
 * will be kept in the buffer after each <code>feedTo</code> is done and will be available for subsequent
 * calls.
 */
public class Feeder {
    
    Buffer buffer;
    InputStream input;
    
    /**
     * Constructs an object.
     */
    public Feeder(Buffer buffer, InputStream input) {
        this.buffer = buffer;
        this.input = input;
    }
    
    /**
     * Feeds the input to a <code>Processor</code> until it finishes its work.
     */
    public void feedTo(Processor processor) {
        byte[] data = buffer.getData();
        
        do {
            
            if (buffer.getLength() > 0) {
                // processing current payload and signalling the buffer that the processed data can be discarded
                int bytesProcessed = processor.process(data, buffer.getOffset(), buffer.getLength());
                buffer.markProcessed(bytesProcessed);
            }
            
            if (processor.finished()) {
                return;
            }
        
            // compacting buffer (moving payload to the start of the buffer) to maximize space for input data
            buffer.compact();
            
            // reading new data and notifying buffer about the new content
            try {
                int length = buffer.getLength();
                int bytes = input.read(data, length, data.length - length);
                
                // no more data to read
                if (bytes == -1)
                    return;
                
                buffer.markAppended(bytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        } while (true);
    }
}
