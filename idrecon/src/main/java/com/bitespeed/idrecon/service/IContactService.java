package com.bitespeed.idrecon.service;

import com.bitespeed.idrecon.dto.ContactResponse;

public interface IContactService {
    ContactResponse identifyContact(String email, String phoneNumber);

}
