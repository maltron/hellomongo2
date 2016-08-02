package net.nortlam.research;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Mauricio "Maltron" Leal <maltron@gmail.com> */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Person implements Serializable {

    private static final Logger LOG = Logger.getLogger(Person.class.getName());
    
    public static final String TAG_ID = "_id";
    @XmlElement(name = TAG_ID, required=false, nillable = true)
    private String ID;
    
    public static final String TAG_FIRST_NAME = "firstName";
    @XmlElement(name = TAG_FIRST_NAME, required=true, nillable = false)
    private String firstName;
    
    public static final String TAG_LAST_NAME = "lastName";
    @XmlElement(name = TAG_LAST_NAME, required=true, nillable = false)
    private String lastName;

    public Person() {
    }
    
    public Person(Document document) {
        ObjectId objectID = document.getObjectId("_id");
        if(objectID != null) this.ID = objectID.toString();
        
        this.firstName = document.getString(TAG_FIRST_NAME);
        this.lastName = document.getString(TAG_LAST_NAME);
    }
    
    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.ID);
        hash = 97 * hash + Objects.hashCode(this.firstName);
        hash = 97 * hash + Objects.hashCode(this.lastName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Person other = (Person) obj;
        if (!Objects.equals(this.ID, other.ID)) {
            return false;
        }
        if (!Objects.equals(this.firstName, other.firstName)) {
            return false;
        }
        if (!Objects.equals(this.lastName, other.lastName)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        LOG.log(Level.INFO, ">>> toString()");
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if(this.ID != null) builder.add(TAG_ID, this.ID);
        if(this.firstName != null) builder.add(TAG_FIRST_NAME, this.firstName);
        if(this.lastName != null) builder.add(TAG_LAST_NAME, this.lastName);
        
        return builder.build().toString();
    }
    
    public Document toDocument() {
        Document document = new Document();
//        if(this.ID != null) document.append("_id", this.ID);
        if(this.firstName != null) document.append(TAG_FIRST_NAME, this.firstName);
        if(this.lastName != null) document.append(TAG_LAST_NAME, this.lastName);
        
        return document;
    }
}