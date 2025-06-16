package com.example.isp392.service;

import com.example.isp392.model.Publisher;
import com.example.isp392.repository.PublisherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Publisher entity operations
 */
@Service
public class PublisherService {

    private static final Logger log = LoggerFactory.getLogger(PublisherService.class);
    
    private final PublisherRepository publisherRepository;
    
    /**
     * Constructor for dependency injection
     * 
     * @param publisherRepository Repository for publisher data access
     */
    public PublisherService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }
    
    /**
     * Get all publishers
     * 
     * @return List of all publishers
     */
    public List<Publisher> findAll() {
        log.debug("Finding all publishers");
        return publisherRepository.findAll();
    }
    
    /**
     * Get all publishers with pagination
     * 
     * @param pageable Pagination and sorting information
     * @return Page of publishers
     */
    public Page<Publisher> findAll(Pageable pageable) {
        log.debug("Finding all publishers with pagination");
        return publisherRepository.findAll(pageable);
    }
    
    /**
     * Find publisher by ID
     * 
     * @param id Publisher ID
     * @return Optional containing publisher if found
     */
    public Optional<Publisher> findById(Integer id) {
        log.debug("Finding publisher by ID: {}", id);
        return publisherRepository.findById(id);
    }
    
    /**
     * Save a new publisher
     * 
     * @param publisher Publisher to save
     * @return Saved publisher
     */
    @Transactional
    public Publisher save(Publisher publisher) {
        log.debug("Saving publisher: {}", publisher.getPublisherName());
        return publisherRepository.save(publisher);
    }
    
    /**
     * Update an existing publisher
     * 
     * @param id Publisher ID to update
     * @param publisherDetails Updated publisher details
     * @return Updated publisher or empty optional if not found
     */
    @Transactional
    public Optional<Publisher> update(Integer id, Publisher publisherDetails) {
        log.debug("Updating publisher with ID: {}", id);
        
        return publisherRepository.findById(id).map(existingPublisher -> {
            // Update publisher fields
            existingPublisher.setPublisherName(publisherDetails.getPublisherName());
            existingPublisher.setDescription(publisherDetails.getDescription());
            existingPublisher.setContactInfo(publisherDetails.getContactInfo());
            
            // Save and return updated publisher
            return publisherRepository.save(existingPublisher);
        });
    }
    
    /**
     * Delete a publisher by ID
     * 
     * @param id Publisher ID to delete
     */
    @Transactional
    public void deleteById(Integer id) {
        log.debug("Deleting publisher with ID: {}", id);
        publisherRepository.deleteById(id);
    }
} 