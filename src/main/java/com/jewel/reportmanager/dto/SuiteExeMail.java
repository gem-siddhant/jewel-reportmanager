package com.jewel.reportmanager.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SuiteExeMail {

    private Set<String> to;

    private Set<String> cc;

    private Set<String> bcc;
}
