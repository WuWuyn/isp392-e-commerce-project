package com.example.isp392.service;

import com.example.isp392.model.User;
import com.example.isp392.model.UserAddress;
import com.example.isp392.repository.UserAddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;

    public UserAddressService(UserAddressRepository userAddressRepository) {
        this.userAddressRepository = userAddressRepository;
    }

    public List<UserAddress> findByUser(User user) {
        return userAddressRepository.findByUser(user);
    }

    public UserAddress findDefaultAddress(User user) {
        return userAddressRepository.findByUserAndIsDefaultTrue(user);
    }

    public Optional<UserAddress> findAddressById(Integer addressId) {
        return userAddressRepository.findById(addressId);
    }

    public void saveAddress(UserAddress address) {
        userAddressRepository.save(address);
    }
} 