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

package org.czentral.event;

import java.util.Date;

public abstract class EventImpl implements Event {
    
    private Object source;
    
    private int type;
    
    private Date date;
    
    protected EventImpl (Object source) {
        this(source, 0, new Date());
    }
    
    protected EventImpl (Object source, int type) {
        this(source, type, new Date());
    }
    
    protected EventImpl (Object source, int type, Date date) {
        this.source = source;
        this.type = type;
        this.date = date;
    }
    
    public int getType() {
        return type;
    }
    
    public Object getSource() {
        return source;
    }
    
    public Date getDate() {
        return date;
    }
}
