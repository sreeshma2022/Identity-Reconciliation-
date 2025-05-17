package com.bitespeed.idrecon.service;

import com.bitespeed.idrecon.dto.ContactResponse;
import com.bitespeed.idrecon.entity.Contact;
import com.bitespeed.idrecon.repository.IContactRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactServiceImpl implements IContactService {

    private final IContactRepository contactRepository;

    @Override
    public ContactResponse identifyContact(String email, String phoneNumber) {

        // Step 1: Fetch contacts with either email or phone number
        List<Contact> contacts = contactRepository.findByEmailOrPhoneNumber(email, phoneNumber);

        // Step 2: If no existing contacts, create a new primary contact
        if (contacts.isEmpty()) {
            Contact newContact = Contact.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .linkPrecedence("primary")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            contactRepository.save(newContact);
            return buildContactResponse(newContact, Collections.emptyList());
        }

        // Step 3: Identify primary contact
        Contact primaryContact = contacts.stream()
                .filter(c -> "primary".equals(c.getLinkPrecedence()))
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElse(contacts.get(0));

        // Step 4: Get all linked/transitive contacts
        Set<Long> ids = contacts.stream().map(Contact::getId).collect(Collectors.toSet());
        List<Contact> allLinked = new ArrayList<>(contacts);
        List<Contact> transitiveContacts = contactRepository.findAllByLinkedIdInOrIdIn(ids, ids);
        allLinked.addAll(transitiveContacts);
        allLinked = new ArrayList<>(new HashSet<>(allLinked)); // remove duplicates

        // Step 5: Normalize link precedence
        for (Contact contact : allLinked) {
            if (!contact.getId().equals(primaryContact.getId())) {
                if ("primary".equals(contact.getLinkPrecedence())) {
                    contact.setLinkPrecedence("secondary");
                    contact.setLinkedId(primaryContact.getId());
                    contact.setUpdatedAt(LocalDateTime.now());
                    contactRepository.save(contact);
                } else if (contact.getLinkedId() == null) {
                    contact.setLinkedId(primaryContact.getId());
                    contact.setUpdatedAt(LocalDateTime.now());
                    contactRepository.save(contact);
                }
            }
        }

        // Step 6: Check for new info and create secondary contact if needed
        Contact newSecondary = createSecondaryIfNewInfo(email, phoneNumber, allLinked, primaryContact);
        if (newSecondary != null) {
            // Re-fetch all linked contacts to include the new one and any new connections
            Set<Long> updatedIds = new HashSet<>();
            updatedIds.add(primaryContact.getId());
            updatedIds.addAll(
                    contactRepository.findAllByLinkedIdInOrIdIn(
                            Collections.singleton(primaryContact.getId()),
                            Collections.singleton(primaryContact.getId())
                    ).stream().map(Contact::getId).collect(Collectors.toSet())
            );

            allLinked = contactRepository.findAllById(updatedIds);
        }


        // Step 7: Build response
        return buildContactResponse(primaryContact, allLinked);
    }

    private Contact createSecondaryIfNewInfo(String email, String phoneNumber, List<Contact> allLinked, Contact primaryContact) {
        boolean emailExists = allLinked.stream().anyMatch(c -> email != null && email.equals(c.getEmail()));
        boolean phoneExists = allLinked.stream().anyMatch(c -> phoneNumber != null && phoneNumber.equals(c.getPhoneNumber()));

        if (!emailExists || !phoneExists) {
            Contact newContact = Contact.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .linkedId(primaryContact.getId())
                    .linkPrecedence("secondary")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return contactRepository.save(newContact);
        }
        return null;
    }

    private ContactResponse buildContactResponse(Contact primaryContact, List<Contact> contacts) {
        Set<String> emails = new LinkedHashSet<>();
        Set<String> phoneNumbers = new LinkedHashSet<>();
        List<Long> secondaryIds = new ArrayList<>();

        if (primaryContact.getEmail() != null) emails.add(primaryContact.getEmail());
        if (primaryContact.getPhoneNumber() != null) phoneNumbers.add(primaryContact.getPhoneNumber());

        for (Contact contact : contacts) {
            if (!contact.getId().equals(primaryContact.getId())) {
                if (contact.getEmail() != null) emails.add(contact.getEmail());
                if (contact.getPhoneNumber() != null) phoneNumbers.add(contact.getPhoneNumber());
                secondaryIds.add(contact.getId());
            }
        }

        return new ContactResponse(
                primaryContact.getId(),
                new ArrayList<>(emails),
                new ArrayList<>(phoneNumbers),
                secondaryIds
        );
    }
}
