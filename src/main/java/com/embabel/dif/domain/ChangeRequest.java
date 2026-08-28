package com.embabel.dif.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("A user or issue request to change software")
public record ChangeRequest(String text) {
}
