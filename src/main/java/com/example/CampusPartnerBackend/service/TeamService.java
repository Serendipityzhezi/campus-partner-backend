package com.example.CampusPartnerBackend.service;

import com.example.CampusPartnerBackend.modal.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.CampusPartnerBackend.modal.domain.User;
import com.example.CampusPartnerBackend.modal.dto.TeamQuery;
import com.example.CampusPartnerBackend.modal.request.TeamJoinRequest;
import com.example.CampusPartnerBackend.modal.request.TeamQuitRequest;
import com.example.CampusPartnerBackend.modal.request.TeamUpdateRequest;
import com.example.CampusPartnerBackend.modal.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 13425
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-03-15 11:31:34
*/
public interface TeamService extends IService<Team> {
    Long addTeam(Team team, User loginUser);


    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);


    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUSer);

    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(Long id, User loginUser);
}
