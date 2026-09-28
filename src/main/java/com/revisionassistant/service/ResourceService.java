package com.revisionassistant.service;

import com.revisionassistant.dao.ResourceDAO;
import com.revisionassistant.dao.SubjectDAO;
import com.revisionassistant.model.Resource;

import java.sql.SQLException;
import java.util.List;

/**
 * Validation and business rules for saved study resources. Controllers
 * talk to this class instead of the DAO directly, so no SQL ever needs
 * to appear in a controller.
 */
public class ResourceService {

    private final ResourceDAO resourceDAO;
    private final SubjectDAO subjectDAO;

    public ResourceService() {
        this.resourceDAO = new ResourceDAO();
        this.subjectDAO = new SubjectDAO();
    }

    /** {@code subjectId} may be {@code null} - a resource does not have to belong to a subject. */
    public Resource addResource(Integer subjectId, String title, String url, String description, String category)
            throws SQLException {
        validateTitle(title);
        validateUrl(url);
        if (subjectId != null) {
            validateSubject(subjectId);
        }
        Resource resource = new Resource(subjectId, title.trim(), url.trim(),
                description == null ? "" : description.trim(), category);
        return resourceDAO.insert(resource);
    }

    public List<Resource> getAllResources() throws SQLException {
        return resourceDAO.findAll();
    }

    public void deleteResource(int resourceId) throws SQLException {
        resourceDAO.delete(resourceId);
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a title.");
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a URL.");
        }
    }

    private void validateSubject(int subjectId) throws SQLException {
        if (subjectDAO.findById(subjectId) == null) {
            throw new IllegalArgumentException("Please select a valid subject.");
        }
    }
}
