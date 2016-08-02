package net.nortlam.research.setup;

import com.mongodb.MongoClient;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 *
 * @author Mauricio "Maltron" Leal <maltron@gmail.com> */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class MongoProvider {

    private static final Logger LOG = Logger.getLogger(MongoProvider.class.getName());
    
    private MongoClient client;
    
    @PostConstruct
    private void init() {
        LOG.log(Level.INFO, ">>> init() CONNECTING TO MONGO");
        client = new MongoClient("localhost", 27017);
    }
    
    @Lock(LockType.READ)
    public MongoClient getClient() {
        return client;
    }

}
