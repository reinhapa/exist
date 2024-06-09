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
package org.exist.xmldb.txn.bridge;

import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.xmldb.LocalChildCollection;
import org.exist.xmldb.LocalCollection;
import org.exist.xmldb.XmldbURI;
import org.exist.xmldb.function.LocalXmldbFunction;
import org.xmldb.api.base.ChildCollection;
import org.xmldb.api.base.XMLDBException;

public class InTxnLocalChildCollection  extends LocalChildCollection implements ChildCollection {
    public InTxnLocalChildCollection(final Subject user, final BrokerPool brokerPool, final LocalCollection parent, final XmldbURI name) throws XMLDBException {
        super(user, brokerPool, parent, name);
    }

    @Override
    protected <R> R withDb(final LocalXmldbFunction<R> dbOperation) throws XMLDBException {
        return InTxnLocalOperations.withDb(brokerPool, user, dbOperation);
    }

    @Override
    public org.xmldb.api.base.Collection getParentCollection() throws XMLDBException {
        if(getName().equals(XmldbURI.ROOT_COLLECTION)) {
            return null;
        }

        if(collection == null) {
            final XmldbURI parentUri = this.<XmldbURI>read().apply((collection, broker, transaction) -> collection.getParentURI());
            this.collection = new InTxnLocalCollection(user, brokerPool, null, parentUri);
        }
        return collection;
    }
}
