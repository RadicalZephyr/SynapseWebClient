package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.controller.AbstractEntityController;

/**
 * Implementation for REST controller for CRUD operations on Base DTOs and Base
 * DAOs
 * <p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed model
 * objects. See controllers specific to particular models for any special
 * handling.
 * 
 * @author deflaux
 * @param <T>
 */
public class DAOControllerImp<T extends Base> implements
		AbstractEntityController<T> {

	private static final Logger log = Logger.getLogger(DAOControllerImp.class
			.getName());

	protected Class<T> theModelClass;
	protected BaseDAO<T> dao;

	/**
	 * @param theModelClass
	 */
	@SuppressWarnings("unchecked")
	public DAOControllerImp(Class<T> theModelClass) {
		this.theModelClass = theModelClass;
		// TODO @Autowired, no GAE references allowed in this class
		DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
		this.dao = daoFactory.getDAO(theModelClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntities
	 * (java.lang.Integer, java.lang.Integer,
	 * javax.servlet.http.HttpServletRequest)
	 */
	public PaginatedResults<T> getEntities(Integer offset, Integer limit,
			HttpServletRequest request) throws DatastoreException {

		ServiceConstants.validatePaginationParams(offset, limit);

		List<T> entities = dao.getInRange(offset - 1, offset + limit - 1);
		Integer totalNumberOfEntities = dao.getCount();

		return new PaginatedResults<T>(request.getServletPath()
				+ UrlHelpers.getUrlForModel(theModelClass), entities,
				totalNumberOfEntities, offset, limit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntity
	 * (java.lang.String)
	 */
	public T getEntity(String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException {

		T entity = dao.get(id);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + id + " exists");
		}

		entity.setUri(UrlHelpers.makeEntityUri(entity, request));
		entity.setEtag(UrlHelpers.makeEntityEtag(entity));

		return entity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity
	 * (T)
	 */
	public T createEntity(T newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException {

		dao.create(newEntity);

		newEntity.setUri(UrlHelpers.makeEntityUri(newEntity, request));
		newEntity.setEtag(UrlHelpers.makeEntityEtag(newEntity));

		return newEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity
	 * (java.lang.String, java.lang.Integer, T)
	 */
	public T updateEntity(String id, Integer etag, T updatedEntity,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		T entity = dao.get(entityId);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}
		if (etag != entity.hashCode()) {
			throw new ConflictingUpdateException(
					"entity with id "
							+ entityId
							+ " was updated since you last fetched it, retrieve it again and reapply the update");
		}
		dao.update(updatedEntity);

		updatedEntity.setUri(UrlHelpers.makeEntityUri(updatedEntity, request));
		updatedEntity.setEtag(UrlHelpers.makeEntityEtag(updatedEntity));

		return updatedEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity
	 * (java.lang.String)
	 */
	public void deleteEntity(String id) throws NotFoundException,
			DatastoreException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		dao.delete(entityId);

		return;
	}

}