/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage.test;

import java.io.File;

import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.dom.DocumentImpl;
import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.xml.sax.InputSource;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * @author wolf
 *
 */
public class RemoveCollectionTest extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(RemoveCollectionTest.class);
    }
    
    private BrokerPool pool;
    
    public void testStore() {
    	BrokerPool.FORCE_CORRUPTION = true;
        DBBroker broker = null;
        try {
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);                       
            TransactionManager transact = pool.getTransactionManager();
            assertNotNull(transact);   
            Txn transaction = transact.beginTransaction();
            assertNotNull(transaction);   
            System.out.println("Transaction started ...");
            
            Collection test = broker.getOrCreateCollection(transaction, DBBroker.ROOT_COLLECTION + "/test");
            assertNotNull(test);   
            broker.saveCollection(transaction, test);
            
            Collection test2 = broker.getOrCreateCollection(transaction, DBBroker.ROOT_COLLECTION + "/test/test2");
            assertNotNull(test2);   
            broker.saveCollection(transaction, test2);
            
            File f = new File("samples/biblio.rdf");
            assertNotNull(f);   
            InputSource is = new InputSource(f.toURI().toASCIIString());
            assertNotNull(is);   
            IndexInfo info = test.validate(transaction, broker, "biblio.rdf", is);
            assertNotNull(info);   
            test.store(transaction, broker, info, is, false);
            
            transact.commit(transaction);
            System.out.println("Transaction commited ...");
            
            transaction = transact.beginTransaction();
            System.out.println("Transaction started ...");
            
            broker.removeCollection(transaction, test);
            
//          Don't commit...
            transact.getJournal().flushToLog(true);
            System.out.println("Transaction interrupted ...");
	    } catch (Exception e) {            
	        fail(e.getMessage());               
        } finally {
        	if (pool != null) pool.release(broker);
        }
    }
    
    public void testRead() {
        BrokerPool.FORCE_CORRUPTION = false;
        DBBroker broker = null;
        try {
        	System.out.println("testRead() ...\n");
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);
            Serializer serializer = broker.getSerializer();
            serializer.reset();
            
            Collection test = broker.openCollection(DBBroker.ROOT_COLLECTION + "/test", Lock.READ_LOCK);            
            assertNotNull("Collection '" + DBBroker.ROOT_COLLECTION +  "/test' not found", test);
            
            DocumentImpl doc = broker.getXMLResource(DBBroker.ROOT_COLLECTION + "/test/biblio.rdf", Lock.READ_LOCK);
            assertNotNull("Document '" + DBBroker.ROOT_COLLECTION +  "/test/biblio.rdf' should not be null", doc);
            String data = serializer.serialize(doc);
            assertNotNull(data);
            System.out.println(data);
            doc.getUpdateLock().release(Lock.READ_LOCK);
            test.release();
	    } catch (Exception e) {            
	        fail(e.getMessage());   
	    } finally {
        	if (pool != null) pool.release(broker);
        }
    }
    
    protected void setUp() {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
            pool = BrokerPool.getInstance();
        } catch (Exception e) {            
            fail(e.getMessage());
        }
    }

    protected void tearDown() {
        BrokerPool.stopAll(false);
    }
}
