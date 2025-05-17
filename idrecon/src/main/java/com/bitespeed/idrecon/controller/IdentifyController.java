package com.bitespeed.idrecon.controller;

import com.bitespeed.idrecon.constants.Constants;
import com.bitespeed.idrecon.dto.ContactResponse;
import com.bitespeed.idrecon.dto.IdentifyRequest;
import com.bitespeed.idrecon.exception.InvalidContactRequestException;
import com.bitespeed.idrecon.service.ContactServiceImpl;
import com.bitespeed.idrecon.util.CommonUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/identify")
public class IdentifyController {

    @Autowired
    private ContactServiceImpl contactService;




    @PostMapping
    public ResponseEntity identify(@RequestBody IdentifyRequest request) {

        String email = request.getEmail();
        String phoneNumber = request.getPhoneNumber();

        if(CommonUtilities.isEmptyCheck(email) && CommonUtilities.isEmptyCheck(phoneNumber)){
            throw new InvalidContactRequestException(Constants.invalidRequest);
        }


        ContactResponse response = contactService.identifyContact(email, phoneNumber);
        return ResponseEntity.ok(Collections.singletonMap("contact", response));
    }
}
