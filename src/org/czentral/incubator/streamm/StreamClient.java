package org.czentral.incubator.streamm;

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

import java.io.*;
import java.nio.ByteBuffer;
import org.czentral.util.stream.Buffer;
    
public class StreamClient {
    
    private Stream stream;
    private OutputStream output;
    
    private boolean runs = true;
    
    
    private boolean sendHeader = true;
    
    private int requestedFragmentSequence = -1;
    
    private boolean sendSingleFragment = false;
    
    
    public StreamClient(Stream stream, OutputStream output) {
        this.stream = stream;
        this.output = output;
    }

    
    public void setSendHeader(boolean sendHeader) {
        this.sendHeader = sendHeader;
    }

    public void setRequestedFragmentSequence(int requestedFragmentSequence) {
        this.requestedFragmentSequence = requestedFragmentSequence;
    }

    public void setSendSingleFragment(boolean sendSingleFragment) {
        this.sendSingleFragment = sendSingleFragment;
    }

    
    public void run() {
        
        // report the starting of this client
        stream.postEvent(new ServerEvent(this, stream, ServerEvent.CLIET_START));
        
        if (sendHeader) {
            
            // waiting for header
            byte[] header = stream.getHeader();
            while (header == null && stream.isRunning()) {

                // sleeping 200 ms
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }

                header = stream.getHeader();
            }

            // writing header
            try {
                output.write(header);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


            /* Web browsers may try to detect some features of the server and open
             * multiple connections. They may close the connection eventually so
             * we wait for a little bit to ensure this request is serious. (And the
             * client needs more than just the header).
             */

            // sleeping 1000 ms
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            
        }
        
        // sending fragments
        int nextSequence = requestedFragmentSequence;
        while (runs && stream.isRunning()) {
            
            boolean fragmentSent = false;
                    
            // while there is a new fragment is available
            int streamAge;
            while (runs 
                    && stream.isRunning()
                    && nextSequence <= (streamAge = stream.getFragmentAge())) {
                
                // notification if a fragment was skipped
                if (nextSequence > 0 && streamAge - nextSequence > 1)
                    stream.postEvent(new ServerEvent(this, stream, ServerEvent.CLIET_FRAGMENT_SKIP));
                
                nextSequence = streamAge + 1;
                
                // getting current movie fragment
                MovieFragment fragment = stream.getFragment();
                
                // send the fragment data to the client
                try {
                    
                    Buffer[] buffers = fragment.getBuffers();
                    for (Buffer buffer : buffers) {
                        
                        // writing data packet
                        output.write(buffer.getData(), buffer.getOffset(), buffer.getLength());

                    }
                    
                    fragmentSent = true;
                    
                    // only one fragment requested: 
                    if (sendSingleFragment) {
                        runs = false;
                        continue;
                    }
            
                } catch (Exception e) {
                    
                    // closed connection
                    runs = false;
                }
            }
                        
            // currently no new fragment, sleeping 200 ms
            if (!fragmentSent) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
            }
            
        }
        
        // report the stopping of thes client
        stream.postEvent(new ServerEvent(this, stream, ServerEvent.CLIET_STOP));
        
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
