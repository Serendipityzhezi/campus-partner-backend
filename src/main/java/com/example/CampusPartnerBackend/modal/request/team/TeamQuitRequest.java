package com.example.CampusPartnerBackend.modal.request.team;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {
    private static final long serialVersionUID = -2038884913144640407L;
    /**
     * id
     */
    private Long teamId;
}