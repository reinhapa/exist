/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.debuggee.dbgp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.exist.debuggee.Packet;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class ResponseEncoder extends ProtocolEncoderAdapter {

    private final static Logger LOG = LogManager.getLogger(ResponseEncoder.class);

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		Packet packet = (Packet) message;
		
		byte[] response = packet.responseBytes();
		String length = String.valueOf(response.length);

		if (LOG.isDebugEnabled())
			LOG.debug("" + length + " byte(s) : " + packet.toString());
		
		IoBuffer buffer = IoBuffer.allocate(response.length+length.length()+2, false);
		buffer.put(length.getBytes());
		buffer.put((byte)0);
		buffer.put(response);
		buffer.put((byte)0);
		buffer.flip();
		
		out.write(buffer);
	}

}
