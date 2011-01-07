package org.sagebionetworks.repo.model;

import java.net.URL;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
/**
 * 
 * @author bhoff
 *
 */
@PersistenceCapable(detachable = "true")
public class Project {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key id; 
	
	@Persistent
	private String name;
	
	public enum Status { PROPOSED, IN_PROGRESS, COMPLETED }
	
	@Persistent
	private Status status;
	
	@Persistent
	private Text overview;
	
	@Persistent
	private Date started;
	
	@Persistent(serialized="true")
	private URL sharedDocs;

	public Key getId() {
		return id;
	}

	public void setId(Key id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Text getOverview() {
		return overview;
	}

	public void setOverview(Text overview) {
		this.overview = overview;
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public URL getSharedDocs() {
		return sharedDocs;
	}

	public void setSharedDocs(URL sharedDocs) {
		this.sharedDocs = sharedDocs;
	}
	
}