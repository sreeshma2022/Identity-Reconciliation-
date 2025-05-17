package com.bitespeed.idrecon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentifyRequest {

    private String email;
    private String phoneNumber;

}
