package com.bitespeed.idrecon.repository;

import com.bitespeed.idrecon.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;


public interface IContactRepository extends JpaRepository<Contact , Long> {

    @Query("SELECT c FROM Contact c WHERE c.email = :email OR c.phoneNumber = :phoneNumber")
    List<Contact> findByEmailOrPhoneNumber(@Param("email") String email, @Param("phoneNumber") String phoneNumber);

    List<Contact> findAllByLinkedIdInOrIdIn(Set<Long> linkedIds, Set<Long> selfIds);

}
