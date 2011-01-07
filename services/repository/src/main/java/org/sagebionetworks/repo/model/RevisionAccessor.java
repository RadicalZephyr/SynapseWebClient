package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;

public interface RevisionAccessor<T extends Revisable<T>> {
	
	public Revision<T> getRevision(Key id);
	
	public Revision<T> getLatest(Revision<T> original);
	
	// Revision should be under an 'owned' relationship with another persistent object
	// as such it should be created and deleted when the parent is.
	
//	public void makePersistent(Revision<T> revision);
//	
//	public void delete(Revision<T> revision);
}