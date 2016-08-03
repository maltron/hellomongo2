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
        
        return Response.status(Response.Status.ACCEPTED).build();
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

    @GET @Path("/docs")
    @Produces(MediaType.TEXT_HTML)
    public String docs() {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
"\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
"<html>\n" +
"<head>\n" +
"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">\n" +
"<title>Untitled Document</title>\n" +
"<style type=\"text/css\">\n" +
"<!--\n" +
".style1 {\n" +
"	font-family: \"Courier New\", Courier, mono;\n" +
"	font-weight: bold;\n" +
"}\n" +
".style3 {color: #FFFFFF}\n" +
"-->\n" +
"</style>\n" +
"</head><table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td>&nbsp;</td>\n" +
"  </tr>\n" +
"</table>\n" +
"\n" +
"\n" +
"<body>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <td width=\"40\" bgcolor=\"#000000\"><h1 align=\"center\" class=\"style3\">GET</h1></td>\n" +
"    <td><h1>/</h1></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <td colspan=\"2\"><h2>List all Persons </h2></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th width=\"50%\" bgcolor=\"#FFFF00\" scope=\"col\">Consumes</th>\n" +
"    <th width=\"50%\" bgcolor=\"#00FF00\" scope=\"col\">Produces</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td width=\"50%\" bgcolor=\"#FFFF00\">&nbsp;</td>\n" +
"    <td width=\"50%\" bgcolor=\"#00FF00\" class=\"style1\">-H Accept: application/json </td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Request</th>\n" +
"    <td colspan=\"2\"><span class=\"style1\"># curl -X GET -i -H &quot;Accept: application/json&quot; http//&lt;server&gt;:&lt;port&gt;/ </span></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Response</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#CCCCCC\" class=\"style1\">HTTP/1.1 200 OK<br>\n" +
"Connection: keep-alive<br>\n" +
"X-Powered-By: Undertow/1<br>\n" +
"Server: WildFly/10<br>\n" +
"Content-Type: application/json<br>\n" +
"Content-Length: 634<br>\n" +
"Date: Wed, 03 Aug 2016 14:45:07 GMT\n" +
"<p>[{&quot;_id&quot;:{&quot;timestamp&quot;:1470173022,&quot;machineIdentifier&quot;:9846577,&quot;processIdentifier&quot;:6728,&quot;counter&quot;:16385041,&quot;timeSecond&quot;:1470173022...</p></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">ERROR</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#FF0000\">&nbsp;</td>\n" +
"  </tr>\n" +
"</table>\n" +
"<p>&nbsp; </p>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"    <tr>\n" +
"    <td width=\"15\" bgcolor=\"#000000\"><h1 align=\"center\" class=\"style3\">GET</h1></td>\n" +
"    <td><h1>/{ID}</h1></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <td colspan=\"2\"><h2>Brings a specific Person by on his ID </h2></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th width=\"50%\" bgcolor=\"#FFFF00\" scope=\"col\">Consumes</th>\n" +
"    <th width=\"50%\" bgcolor=\"#00FF00\" scope=\"col\">Produces</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td width=\"50%\" bgcolor=\"#FFFF00\">&nbsp;</td>\n" +
"    <td width=\"50%\" bgcolor=\"#00FF00\" class=\"style1\">-H Accept: application/json </td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Request</th>\n" +
"    <td colspan=\"2\"><span class=\"style1\"># curl -X GET -i -H &quot;Accept: application/json&quot; http//&lt;server&gt;:&lt;port&gt;/57a12a2e963f311a482e069a</span></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Response</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#CCCCCC\" class=\"style1\"><p>HTTP/1.1 200 OK<br>\n" +
"      Connection: keep-alive<br>\n" +
"      X-Powered-By: Undertow/1<br>\n" +
"      Server: WildFly/10<br>\n" +
"      Content-Type: application/json<br>\n" +
"      Content-Length: 75<br>\n" +
"      Date: Wed, 03 Aug 2016 14:53:45 GMT</p>\n" +
"    <p>{&quot;_id&quot;:&quot;57a12a2e963f311a482e069a&quot;,&quot;firstName&quot;:&quot;Mauricio&quot;,&quot;lastName&quot;:&quot;Leal&quot;}</p></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">ERROR</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#FF0000\" style=\"style3\"><span class=\"style3\"><strong>400: Bad Request </strong> - If the ID is not well format<br/>\n" +
"	    <strong>404: Resource not Found</strong> - Unable to locate this ID</span></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<p>&nbsp;</p>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <td width=\"40\" bgcolor=\"#000000\"><h1 align=\"center\" class=\"style3\">POST</h1></td>\n" +
"    <td><h1>/</h1></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <td colspan=\"2\"><h2>Create a new Person </h2></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th width=\"50%\" bgcolor=\"#FFFF00\" scope=\"col\">Consumes</th>\n" +
"    <th width=\"50%\" bgcolor=\"#00FF00\" scope=\"col\">Produces</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td width=\"50%\" bgcolor=\"#FFFF00\"><span class=\"style1\">-H Content-type: application/json </span></td>\n" +
"    <td width=\"50%\" bgcolor=\"#00FF00\" class=\"style1\">-H Accept: application/json </td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Request</th>\n" +
"    <td colspan=\"2\"><span class=\"style1\"># curl -X POST -i -H &quot;Content-type: application/json&quot; -H &quot;Accept: application/json&quot; http//&lt;server&gt;:&lt;port&gt;/ \\<br/>\n" +
"	-d &quot;{\\&quot;firstName\\&quot;:\\&quot;Mauricio\\&quot;,\\&quot;lastName\\&quot;:\\&quot;Leal\\&quot;}&quot; </span></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Response</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#CCCCCC\" class=\"style1\">HTTP/1.1 201 Created<br>\n" +
"      Connection: keep-alive<br>\n" +
"      X-Powered-By: Undertow/1<br>\n" +
"      Server: WildFly/10<br>\n" +
"      Content-Type: application/json<br>\n" +
"      Content-Length: 75<br>\n" +
"      Date: Wed, 03 Aug 2016 16:38:44 GMT\n" +
"      <p>{&quot;_id&quot;:&quot;57a21e14394921563aaa8293&quot;,&quot;firstName&quot;:&quot;Mauricio&quot;,&quot;lastName&quot;:&quot;Leal&quot;}</p>\n" +
"    </td></tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">ERROR</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#FF0000\"><span class=\"style3\"><strong>400 BAD REQUEST:</strong> If there is no content coming from the request. <br>\n" +
"        <strong>406 NOT ACCEPTABLE:</strong> If the JSON Content is mal format<br>\n" +
"        <strong>500 INTERNAL SERVER ERROR:</strong> The Service was unable to insert the content, possible due a data duplication.\n" +
"    </span></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<p>&nbsp;</p>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <td width=\"40\" bgcolor=\"#000000\"><h1 align=\"center\" class=\"style3\">PUT</h1></td>\n" +
"    <td><h1>/</h1></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <td colspan=\"2\"><h2>Update an existing Person </h2></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th width=\"50%\" bgcolor=\"#FFFF00\" scope=\"col\">Consumes</th>\n" +
"    <th width=\"50%\" bgcolor=\"#00FF00\" scope=\"col\">Produces</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td width=\"50%\" bgcolor=\"#FFFF00\"><span class=\"style1\">-H Content-type: application/json </span></td>\n" +
"    <td width=\"50%\" bgcolor=\"#00FF00\" class=\"style1\">&nbsp;</td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Request</th>\n" +
"    <td colspan=\"2\"><span class=\"style1\"># curl -X PUT -i -H &quot;Content-type: application/json&quot; http://&lt;server&gt;:&lt;port&gt;/ \\ <br/>\n" +
"	-d &quot;{\\&quot;_id\\&quot;:\\&quot;57a12a2e963f311a482e069a\\&quot;,\\&quot;firstName\\&quot;:\\&quot;Mauricio\\&quot;,\\&quot;lastName\\&quot;:\\&quot;Freitas\\&quot;}&quot;</span></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Response</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#CCCCCC\" class=\"style1\">HTTP/1.1 202 Accepted<br>\n" +
"      Connection: keep-alive<br>\n" +
"      X-Powered-By: Undertow/1<br>\n" +
"      Server: WildFly/10<br>\n" +
"      Content-Length: 0<br>\n" +
"      Date: Wed, 03 Aug 2016 17:38:23 GMT</td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">ERROR</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#FF0000\"><span class=\"style3\"><strong>400 BAD REQUEST:</strong> If there is no content coming from the request, the ID is absent or ID is not a properly formatted. <br>\n" +
"        <strong>404 NOT FOUND:</strong> If the Person wasn't found <br>\n" +
"        <strong>500 INTERNAL SERVER ERROR:</strong> The Service was unable to update the content.\n" +
"    </span></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<p>&nbsp;</p>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <td width=\"40\" bgcolor=\"#000000\"><h1 align=\"center\" class=\"style3\">DELETE</h1></td>\n" +
"    <td><h1>/</h1></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <td colspan=\"2\"><h2>Delete an existing Person </h2></td>\n" +
"  </tr>\n" +
"</table>\n" +
"<table width=\"100%\"  border=\"0\">\n" +
"  <tr>\n" +
"    <th scope=\"col\">&nbsp;</th>\n" +
"    <th width=\"50%\" bgcolor=\"#FFFF00\" scope=\"col\">Consumes</th>\n" +
"    <th width=\"50%\" bgcolor=\"#00FF00\" scope=\"col\">Produces</th>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">&nbsp;</th>\n" +
"    <td width=\"50%\" bgcolor=\"#FFFF00\">&nbsp;</td>\n" +
"    <td width=\"50%\" bgcolor=\"#00FF00\" class=\"style1\">&nbsp;</td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Request</th>\n" +
"    <td colspan=\"2\"><span class=\"style1\"># curl -X DELETE -i http://&lt;server&gt;:&lt;port&gt;/57a12a2e963f311a482e069a</span></td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">Response</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#CCCCCC\" class=\"style1\">HTTP/1.1 200 OK<br>\n" +
"      Connection: keep-alive<br>\n" +
"      X-Powered-By: Undertow/1<br>\n" +
"      Server: WildFly/10<br>\n" +
"      Content-Length: 0<br>\n" +
"    Date: Wed, 03 Aug 2016 17:47:11 GMT</td>\n" +
"  </tr>\n" +
"  <tr>\n" +
"    <th scope=\"row\">ERROR</th>\n" +
"    <td colspan=\"2\" bgcolor=\"#FF0000\"><span class=\"style3\"><strong>400 BAD REQUEST:</strong> If there no ID specified or if the ID is not properly formatted <br>\n" +
"        <strong>404 NOT FOUND:</strong> If the Person wasn't found <br>\n" +
"        <strong>500 INTERNAL SERVER ERROR:</strong> The Service was unable to update the content.\n" +
"    </span></td>\n" +
"  </tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>";
    }
}
