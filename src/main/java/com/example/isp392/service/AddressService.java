package com.example.isp392.service;

import com.example.isp392.dto.AddressDTO;
import com.example.isp392.model.User;
import com.example.isp392.model.UserAddress;
import com.example.isp392.repository.UserAddressRepository;
import com.example.isp392.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing user addresses
 * Contains business logic for CRUD operations on addresses
 */
@Service
public class AddressService {

    // Repositories for data access
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Constructor for dependency injection (Constructor Injection instead of @Autowired)
     * @param addressRepository repository for address operations
     * @param userRepository repository for user operations
     */
    public AddressService(UserAddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all addresses for a specific user
     * @param userId the ID of the user
     * @return list of address DTOs
     */
    public List<AddressDTO> getAllAddressesByUser(int userId) {
        // Find all addresses for the user from repository
        List<UserAddress> addresses = addressRepository.findByUserUserId(userId);
        
        // Convert entities to DTOs using stream API
        return addresses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific address by ID and user ID
     * @param addressId the address ID
     * @param userId the user ID
     * @return the address DTO if found
     */
    public Optional<AddressDTO> getAddressByIdAndUser(int addressId, int userId) {
        // Find specific address for the user and convert to DTO if found
        return addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .map(this::convertToDTO);
    }

    /**
     * Create a new address for a user
     * @param addressDTO the address data
     * @param userId the user ID
     * @return the created address DTO
     */
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO, int userId) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if this is the first address for the user
        boolean isFirstAddress = addressRepository.countByUserUserId(userId) == 0;
        if (isFirstAddress) {
            // First address is automatically set as default
            addressDTO.setDefault(true);
        } else if (addressDTO.isDefault()) {
            // If this address is marked as default, reset other addresses
            addressRepository.resetDefaultAddresses(userId, -1); // -1 as placeholder since address doesn't have ID yet
        }

        // Convert DTO to entity
        UserAddress address = convertToEntity(addressDTO);
        address.setUser(user);
        
        // Save the address to database
        UserAddress savedAddress = addressRepository.save(address);
        
        // Return the saved address as DTO
        return convertToDTO(savedAddress);
    }

    /**
     * Update an existing address
     * @param addressId the address ID to update
     * @param addressDTO the new address data
     * @param userId the user ID
     * @return the updated address DTO
     */
    @Transactional
    public AddressDTO updateAddress(int addressId, AddressDTO addressDTO, int userId) {
        // Find the address
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or doesn't belong to user"));

        // Keep track of default status
        boolean wasDefault = address.isDefault();
        boolean nowDefault = addressDTO.isDefault();
        
        // Update fields from DTO
        address.setRecipientName(addressDTO.getRecipientName());
        address.setRecipientPhone(addressDTO.getRecipientPhone());
        address.setProvince(addressDTO.getProvince());
        address.setDistrict(addressDTO.getDistrict());
        address.setWard(addressDTO.getWard());
        address.setAddressDetail(addressDTO.getAddressDetail());
        address.setCompany(addressDTO.getCompany());
        address.setAddress_type(addressDTO.getAddressType());
        
        // Handle default status
        if (wasDefault) {
            // If this was the default address, ensure it stays default
            address.setDefault(true);
        } else {
            // Otherwise, respect the input
            address.setDefault(nowDefault);
            
            // If changing to default, update other addresses
            if (nowDefault) {
                addressRepository.resetDefaultAddresses(userId, addressId);
            }
        }
        
        // Save the updated address
        UserAddress updatedAddress = addressRepository.save(address);
        return convertToDTO(updatedAddress);
    }

    /**
     * Delete an address
     * @param addressId the address ID to delete
     * @param userId the user ID
     * @return true if deleted successfully
     * @throws RuntimeException if address is default
     */
    @Transactional
    public boolean deleteAddress(int addressId, int userId) {
        // Find the address
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or doesn't belong to user"));
        
        // Check if this is a default address
        if (address.isDefault()) {
            throw new RuntimeException("Cannot delete default address");
        }
        
        // Delete the address
        addressRepository.delete(address);
        return true;
    }

    /**
     * Set an address as default
     * @param addressId the address ID to set as default
     * @param userId the user ID
     * @return the updated address DTO
     */
    @Transactional
    public AddressDTO setDefaultAddress(int addressId, int userId) {
        // Reset all addresses for this user to not default
        addressRepository.resetDefaultAddresses(userId, addressId);
        
        // Set the selected address as default
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or doesn't belong to user"));
        
        address.setDefault(true);
        UserAddress updatedAddress = addressRepository.save(address);
        
        return convertToDTO(updatedAddress);
    }

    /**
     * Convert entity to DTO
     * @param address the address entity
     * @return the address DTO
     */
    private AddressDTO convertToDTO(UserAddress address) {
        AddressDTO dto = new AddressDTO();
        dto.setAddressId(address.getAddressId());
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhone(address.getRecipientPhone());
        dto.setProvince(address.getProvince());
        dto.setDistrict(address.getDistrict());
        dto.setWard(address.getWard());
        dto.setAddressDetail(address.getAddressDetail());
        dto.setCompany(address.getCompany());
        dto.setAddressType(address.getAddress_type());
        dto.setDefault(address.isDefault());
        return dto;
    }

    /**
     * Convert DTO to entity
     * @param dto the address DTO
     * @return the address entity
     */
    private UserAddress convertToEntity(AddressDTO dto) {
        UserAddress address = new UserAddress();
        
        // Don't set ID for new entities (when ID is null)
        if (dto.getAddressId() != null) {
            address.setAddressId(dto.getAddressId());
        }
        
        address.setRecipientName(dto.getRecipientName());
        address.setRecipientPhone(dto.getRecipientPhone());
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setWard(dto.getWard());
        address.setAddressDetail(dto.getAddressDetail());
        address.setCompany(dto.getCompany());
        address.setAddress_type(dto.getAddressType());
        address.setDefault(dto.isDefault());
        
        return address;
    }
}
