/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2024 The eXist-db Authors
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
package org.exist.xmldb;

import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.xmldb.api.base.ChildCollection;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

public class LocalChildCollection extends LocalCollection implements ChildCollection {
    public LocalChildCollection(Subject user, BrokerPool brokerPool, LocalCollection parent, XmldbURI name) throws XMLDBException {
        super(user, brokerPool, parent, name);
    }

    @Override
    public Collection getParentCollection() throws XMLDBException {
        return withDb((broker, transaction) -> {
            if (getName(broker, transaction).equals(XmldbURI.ROOT_COLLECTION)) {
                return null;
            }

            if (collection == null) {
                final XmldbURI parentUri = this.<XmldbURI>read(broker, transaction).apply((collection, broker1, transaction1) -> collection.getParentURI());
                this.collection = new LocalCollection(user, brokerPool, null, parentUri);
            }
            return collection;
        });
    }
}
