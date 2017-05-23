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

import org.czentral.util.stream.Processor;

class HeaderDetectionState implements Processor {
    
    private static final long ID_EBML = 0x1A45DFA3;
    private static final long ID_SEGMENT = 0x18538067;
    private static final long ID_INFO = 0x1549A966;
    private static final long ID_TRACKS = 0x1654AE6B;
    private static final long ID_TRACKTYPE = 0x83;
    private static final long ID_TRACKNUMBER = 0xD7;
    private static final long TRACK_TYPE_VIDEO = 1;
    
    private StreamInput input;
    private Stream stream;
    
    private long videoTrackNumber = 0;
    private boolean finished = false;
    
    private static final byte[] infiniteSegment = {0x18, 0x53, (byte)0x80, 0x67, 0x01, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        
    public HeaderDetectionState(StreamInput input, Stream stream) {
        this.input = input;
        this.stream = stream;
    }

    public long getVideoTrackNumber() {
        return videoTrackNumber;
    }
    
    @Override
    public int process(byte[] buffer, int offset, int length) {
        
        int endOffset = offset + length;
        
        byte[] headerBuffer = new byte[65536];
        int headerLength = 0;
            
        EBMLElement elem;
        
        // EBML root element
        try {
           elem = new EBMLElement(buffer, offset, length);
        } catch (RuntimeException e) {
            // on EBML reading errors, need more data to be loaded
            return 0;
        }
        
        // if not EBML
        if (elem.getId() != ID_EBML)
            throw new RuntimeException("First element is not EBML!");
        
        // COPYING: EBML headerBuffer
        System.arraycopy(buffer, elem.getElementOffset(), headerBuffer, headerLength, elem.getElementSize());
        headerLength += elem.getElementSize();
        
        offset = elem.getEndOffset();
        
        // COPYING: infinite Segment
        System.arraycopy(infiniteSegment, 0, headerBuffer, headerLength, infiniteSegment.length);
        headerLength += infiniteSegment.length;
        
        
        // looking for: Segment
        do {
            elem = new EBMLElement(buffer, offset, length);
            if (elem.getId() == ID_SEGMENT)
                break;
            offset = elem.getEndOffset();
        } while (offset < endOffset);
    
        // if not found ...
        if (offset >= endOffset)
            return 0;
        
        
        int segmentDataOffset = elem.getDataOffset();
        
        // looking for: Info
        offset = segmentDataOffset;
        do {
            elem = new EBMLElement(buffer, offset, length);
            offset = elem.getEndOffset();
        } while (offset < endOffset && elem.getId() != ID_INFO);
    
        // if not found ...
        if (offset >= endOffset)
            return 0;

        // COPYING: Info headerBuffer
        System.arraycopy(buffer, elem.getElementOffset(), headerBuffer, headerLength, elem.getElementSize());
        headerLength += elem.getElementSize();

        
        // looking for: Tracks
        offset = segmentDataOffset;
        do {
            elem = new EBMLElement(buffer, offset, length);
            offset = elem.getEndOffset();
        } while (offset < endOffset && elem.getId() != ID_TRACKS);
    
        // if not found ...
        if (offset >= endOffset)
            return 0;

        // COPYING: Tracks headerBuffer
        System.arraycopy(buffer, elem.getElementOffset(), headerBuffer, headerLength, elem.getElementSize());
        headerLength += elem.getElementSize();
        
        // searching for video track's id
        int endOfTracks = elem.getEndOffset();
        offset = elem.getDataOffset();
        while (offset < endOfTracks) {
            EBMLElement track = new EBMLElement(buffer, offset, endOfTracks - offset);
            offset = track.getDataOffset();
            int endOfTrack = track.getEndOffset();
            
            long trackType = 0;
            long trackNumber = 0;
            while (offset < endOfTrack) {
                EBMLElement property = new EBMLElement(buffer, offset, endOfTrack - offset);
                if (property.getId() == ID_TRACKTYPE) {
                    trackType = buffer[property.getDataOffset()] & 0xff;
                } else if (property.getId() == ID_TRACKNUMBER) {
                    trackNumber = EBMLElement.loadUnsigned(buffer, property.getDataOffset(), (int)property.getDataSize());
                }
                offset = property.getEndOffset();
            }
            
            if (trackType == TRACK_TYPE_VIDEO)
                videoTrackNumber = trackNumber;
            
            offset = track.getEndOffset();
        }
        
        // setting header for the stream
        byte[] header = new byte[headerLength];
        System.arraycopy(headerBuffer, 0, header, 0, headerLength);
        stream.setHeader(header);

        finished = true;
        return segmentDataOffset;
    }

    @Override
    public boolean finished() {
        return finished;
    }
    
}
