package net.nortlam.research;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.util.JSONParseException;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.nortlam.research.setup.MongoProvider;

/**
 *
 * @author Mauricio "Maltron" Leal <maltron@gmail.com> */
@Path("/")
public class Resource {

    private static final Logger LOG = Logger.getLogger(Resource.class.getName());
    
    @EJB
    private MongoProvider provider;
    
    @GET @Path("/docs")
    @Produces(MediaType.TEXT_HTML)
    public String docs() {
        return "<html>"
                + "<body>"
                + "<h1>/</h1>"
                + "List all Persons available<br/>"
                + "<table><tr><td></td><td>Content-type: application/json</td></tr></table>"
                + "</body>"
                + "</html>";
    }
    
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response all() {
        LOG.log(Level.INFO, ">> all()");
        
        List<Document> all = getCollection().find().into(new ArrayList<Document>());
        for(Document document: all) {
            System.out.printf(">>> DOCUMENT:%s ID:%s\n", document.toString(), document.get("_id"));
            System.out.printf(">>> DOCUMENT (JSON):%s\n", document.toJson());
            
            for(String key: document.keySet())
                System.out.printf(">>> KEY:%s  VALUE:%s\n", key, document.get(key));
        }
        
        GenericEntity<List<Document>> result = 
                new GenericEntity<List<Document>>(all){};
        
        return Response.ok(result).build();
    }
    
    @GET @Path("{ID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@PathParam("ID")String ID) {
        LOG.log(Level.INFO, ">>> fetch():{0}", ID);
        
        Person found = null;
        try {
            ObjectId objectID = new ObjectId(ID);
            BasicDBObject query = new BasicDBObject("_id", objectID);
            Document document = getCollection().find(query).first();
            if(document == null)
                return Response.status(Response.Status.NOT_FOUND).build();
            
            found = new Person(document);
            
        } catch(IllegalArgumentException ex) {
            // In case ID it's not an hex properly 
            LOG.log(Level.SEVERE, "### fetch() ILLEGAL ARGUMENT EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        return Response.ok(found, MediaType.APPLICATION_JSON).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Person person) {
        LOG.log(Level.INFO, ">>> create()");
        
        if(person == null) {
            LOG.log(Level.SEVERE, "### create() Parameter PERSON is NULL");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        LOG.log(Level.INFO, ">>> create() ARGUMENT IS NOT NULL");
        LOG.log(Level.INFO, ">>> create():{0}", person.toString());
        Person newPerson = null;
        try {
            Document document = person.toDocument();
            getCollection().insertOne(document);
            
            // Successfully created
            newPerson = new Person(document);
            
        } catch(JSONParseException ex) {
            LOG.log(Level.SEVERE, "### JSON PARSE EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            
        } catch(MongoWriteException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoWriteConcernException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE CONCERN EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoException ex) {
            LOG.log(Level.SEVERE, "### MONGO EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return Response.status(Response.Status.CREATED).entity(newPerson).build();
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response replace(Person person) {
        LOG.log(Level.INFO, ">>> replace()");
        if(person == null) {
            LOG.log(Level.SEVERE, "### create() Parameter PERSON is NULL");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        if(person.getID() == null) {
            LOG.log(Level.SEVERE, "### create() ID is NULL");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        LOG.log(Level.INFO, ">>> replace():{0}", person.toString());

        UpdateResult result = null;
        try {
            result = getCollection().updateOne(
                    new BasicDBObject("_id", new ObjectId(person.getID())),
                    new Document("$set", person.toDocument()));
            if(!result.wasAcknowledged())
                return Response.status(Response.Status.NOT_FOUND).build();
            
        } catch(IllegalArgumentException ex) {
            // In case ID it's not an hex properly 
            LOG.log(Level.SEVERE, "### replace() ILLEGAL ARGUMENT EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();

        } catch(MongoWriteException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoWriteConcernException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE CONCERN EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoException ex) {
            LOG.log(Level.SEVERE, "### MONGO EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        LOG.log(Level.INFO, ">>> replace() Was Acknowledge ? {0} Matched Count:{1} Modified Count:{2} Upserted ID:{3}", 
                new Object[] {result.wasAcknowledged() ? "YES" : "NO", result.getMatchedCount(),
                result.getModifiedCount(), result.getUpsertedId()});
        
        return Response.ok().build();
    }
    
    @DELETE @Path("{ID}")
    public Response delete(@PathParam("ID")String ID) {
        if(ID == null) {
            LOG.log(Level.SEVERE, "### delete() ARGUMENT \'ID\' IS NULL");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        LOG.log(Level.INFO, ">>> delete():{0}", ID);
        
        DeleteResult result = null;
        try {
            result = getCollection().deleteOne(new BasicDBObject("_id", new ObjectId(ID)));
            if(result.getDeletedCount() == 0)
                return Response.status(Response.Status.NOT_FOUND).build();
            
        } catch(IllegalArgumentException ex) {
            // In case ID it's not an hex properly 
            LOG.log(Level.SEVERE, "### delete() ILLEGAL ARGUMENT EXCEPTION:{0}",
                    ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
            
        } catch(MongoWriteException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoWriteConcernException ex) {
            LOG.log(Level.SEVERE, "### MONGO WRITE CONCERN EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch(MongoException ex) {
            LOG.log(Level.SEVERE, "### MONGO EXCEPTION:{0}", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        LOG.log(Level.INFO, ">>> delete() Was Acknowledge ? {0} Deleted Count:{1}", 
                new Object[] {result.wasAcknowledged() ? "YES" : "NO", result.getDeletedCount()});
        
        return Response.ok().build();
    }
    
    private MongoCollection<Document> getCollection() {
        LOG.log(Level.INFO, ">>> getCollection()");
        MongoDatabase database = provider.getClient().getDatabase("myclass88");
        return database.getCollection("persons");
    }
}
