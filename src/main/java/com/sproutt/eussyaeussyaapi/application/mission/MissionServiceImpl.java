package com.sproutt.eussyaeussyaapi.application.mission;

import com.sproutt.eussyaeussyaapi.api.mission.dto.MissionDTO;
import com.sproutt.eussyaeussyaapi.api.mission.dto.MissionResponseDTO;
import com.sproutt.eussyaeussyaapi.domain.member.Member;
import com.sproutt.eussyaeussyaapi.domain.mission.Mission;
import com.sproutt.eussyaeussyaapi.domain.mission.MissionRepository;
import com.sproutt.eussyaeussyaapi.domain.mission.MissionStatus;
import com.sproutt.eussyaeussyaapi.domain.mission.exceptions.ExpiredMissionException;
import com.sproutt.eussyaeussyaapi.domain.mission.exceptions.NoPermissionException;
import com.sproutt.eussyaeussyaapi.domain.mission.exceptions.NoSuchMissionException;
import com.sproutt.eussyaeussyaapi.domain.mission.exceptions.NotSatisfiedCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;

    @Override
    public Mission create(Member loginMember, MissionDTO missionDTO) {
        Mission mission = new Mission(loginMember, missionDTO);

        return missionRepository.save(mission);
    }

    @Override
    public Mission update(Member loginMember, Long missionId, MissionDTO missionDTO) {
        Mission mission = missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);

        if (!mission.isWriter(loginMember)) {
            throw new RuntimeException();
        }

        return missionRepository.save(mission.update(missionDTO));
    }

    @Override
    public void delete(Member loginMember, Long missionId) {
        Mission mission = missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);

        if (!mission.isWriter(loginMember)) {
            throw new NoPermissionException();
        }

        missionRepository.delete(mission);
    }

    @Override
    public Mission findById(Long missionId) {
        return missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);
    }

    @Override
    public List<Mission> findByWriter(Member writer) {
        List<Mission> missionList = missionRepository.findAllByWriter(writer);

        return missionList;
    }

    @Override
    public List<Mission> findAll() {
        List<Mission> missionList = missionRepository.findAll();

        return missionList;
    }

    @Override
    public List<Mission> filterDate(String afterDate, String beforeDate, List<Mission> missionList) {
        if (afterDate != null) {
            LocalDateTime afterLocalDateTime = LocalDateTime.ofInstant(Instant.parse(afterDate), ZoneId.of("Asia/Seoul"));
            missionList = missionList.stream()
                                     .filter(mission -> mission.getDeadlineTime().isAfter(afterLocalDateTime))
                                     .collect(Collectors.toList());
        }

        if (beforeDate != null) {
            LocalDateTime beforeLocalDateTime = LocalDateTime.ofInstant(Instant.parse(beforeDate), ZoneId.of("Asia/Seoul"));
            missionList = missionList.stream()
                                     .filter(mission -> mission.getDeadlineTime().isBefore(beforeLocalDateTime))
                                     .collect(Collectors.toList());
        }

        return missionList;
    }

    @Override
    @Transactional
    public void pauseMission(Member loginMember, Long missionId, LocalDateTime stopPointTime) {
        Mission mission = missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);

        if (!mission.isWriter(loginMember)) {
            throw new NoPermissionException();
        }

        if (!mission.isToday(stopPointTime)) {
            throw new ExpiredMissionException();
        }

        mission.recordPauseTime(stopPointTime);
        mission.updateRunningTime();
        mission.pause();
        missionRepository.save(mission);
    }

    @Override
    @Transactional
    public void startMission(Member loginMember, Long missionId, LocalDateTime startPointTime) {
        Mission mission = missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);

        if (!mission.isWriter(loginMember)) {
            throw new NoPermissionException();
        }

        if (!mission.isToday(startPointTime)) {
            throw new ExpiredMissionException();
        }

        mission.recordStartTime(startPointTime);
        mission.start();
        missionRepository.save(mission);
    }

    @Override
    public void completeMission(Member loginMember, Long missionId, LocalDateTime now) {
        Mission mission = missionRepository.findById(missionId).orElseThrow(NoSuchMissionException::new);

        if (!mission.isWriter(loginMember)) {
            throw new NoPermissionException();
        }

        if (!mission.isDeadlinePassed(now)) {
            throw new NotSatisfiedCondition();
        }

        mission.recordPauseTime(now);
        mission.updateRunningTime();
        mission.complete();
        missionRepository.save(mission);
    }

    @Override
    public List<Mission> filterStatus(String status, List<Mission> missions) {
        if (status == null) {
            return missions;
        }

        if (status.equals("UNCOMPLETE")) {
            return missions.stream().filter(mission -> mission.getStatus() != MissionStatus.COMPLETE).collect(Collectors.toList());
        }

        return missions.stream().filter(mission -> mission.getStatus().name().equals(status)).collect(Collectors.toList());
    }

    public List<MissionResponseDTO> changeResponseDTOList(List<Mission> missionList) {
        List<MissionResponseDTO> responseList = new ArrayList<>();
        missionList.forEach(mission -> responseList.add(new MissionResponseDTO(mission)));

        return responseList;
    }
}
