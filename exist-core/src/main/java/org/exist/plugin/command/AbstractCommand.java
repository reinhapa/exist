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
package org.exist.plugin.command;

import java.io.PrintStream;

import org.exist.xmldb.XmldbURI;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public abstract class AbstractCommand implements Command {
	
	private PrintStream out = System.out;
	private PrintStream err = System.err;
	
	protected String[] names = null;
	
	public String[] getNames() {
		return names;
	}
	
	public PrintStream out() {
		return out;
	}

	public PrintStream err() {
		return err;
	}

	/* (non-Javadoc)
	 * @see org.exist.plugin.command.Command#process(java.lang.String[])
	 */
	@Override
	public void process(String[] params) throws CommandException {
		process(XmldbURI.ROOT_COLLECTION_URI, params);
	}

	/* (non-Javadoc)
	 * @see org.exist.plugin.command.Command#process(org.exist.xmldb.XmldbURI, java.lang.String[])
	 */
	@Override
	public abstract void process(XmldbURI collection, String[] commandData) throws CommandException;

}
