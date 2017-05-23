/*
 * This file is part of the "stream-m" software. An HTML5 compatible live
 * streaming server.
 * Copyright (C) 2014 Varga Bence
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.czentral.incubator.streamm.web;

import java.util.Map;
import java.util.Properties;
import org.czentral.incubator.streamm.ControlledStream;
import org.czentral.incubator.streamm.MatroskaFragment;
import org.czentral.incubator.streamm.MovieFragment;
import org.czentral.incubator.streamm.Stream;
import org.czentral.minihttp.HTTPException;
import org.czentral.minihttp.HTTPRequest;
import org.czentral.minihttp.HTTPResource;
import org.czentral.minihttp.HTTPResponse;

/**
 *
 * @author Varga Bence
 */
public class SnapshotResource implements HTTPResource {

    private final String STR_CONTENT_TYPE = "Content-type";
    
    protected Properties props;

    protected Map<String, ControlledStream> streams;

    public SnapshotResource(Properties props, Map<String, ControlledStream> streams) {
        this.props = props;
        this.streams = streams;
    }

    public void serve(HTTPRequest request, HTTPResponse response) throws HTTPException {
        // the part of the path after the resource's path
        int resLength = request.getResourcePath().length();
        String requestPath = request.getPathName();
        if (requestPath.length() - resLength <= 1) {
            throw new HTTPException(400, "No Stream ID Specified");
        }
        // Stream ID
        String streamID = requestPath.substring(resLength + 1);
        // is a stream with that Stream ID defined?
        if (props.getProperty("streams." + streamID) == null) {
            throw new HTTPException(404, "Stream Not Registered");
        }
        // getting stream
        Stream stream = streams.get(streamID);
        if (stream == null || !stream.isRunning()) {
            throw new HTTPException(503, "Stream Not Running");
        }
        // setting rsponse content-type
        response.setParameter(STR_CONTENT_TYPE, "image/webp");
        // getting current fragment
        if (!(stream.getFragment() instanceof MatroskaFragment)) {
            throw new HTTPException(404, "Not a Matroska stream.");
        }
        MatroskaFragment fragment = (MatroskaFragment)stream.getFragment();
        // check if there is a fragment available
        if (fragment == null) {
            throw new HTTPException(404, "No Fragment Found");
        }
        // check if there is a keyframe available
        if (fragment.getKeyBuffer() == null) {
            throw new HTTPException(404, "No Keyframe Found");
        }
        // RIFF header
        byte[] header = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ', 0, 0, 0, 0};
        int offset;
        int num;
        // saving data length
        offset = 7;
        num = fragment.getKeyBuffer().getLength() + 12;
        while (num > 0) {
            header[offset--] = (byte) num;
            num >>= 8;
        }
        // saving payload length
        offset = 19;
        num = fragment.getKeyBuffer().getLength();
        while (num > 0) {
            header[offset--] = (byte) num;
            num >>= 8;
        }
        try {
            // sending header
            response.getOutputStream().write(header, 0, header.length);
            // sending data
            response.getOutputStream().write(fragment.getKeyBuffer().getData(), fragment.getKeyBuffer().getOffset(), fragment.getKeyBuffer().getLength());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
