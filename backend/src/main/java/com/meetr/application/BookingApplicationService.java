package com.meetr.application;

import com.meetr.application.dto.BookingAttendeeDTO;
import com.meetr.application.dto.BookingConflictDTO;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.BookingResult;
import com.meetr.application.dto.ConflictCheckRequest;
import com.meetr.application.dto.ConflictCheckResponse;
import com.meetr.application.dto.CreateBookingCommand;
import com.meetr.application.dto.UpdateBookingCommand;
import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.BookingAttendee;
import com.meetr.domain.entity.BookingOperationLog;
import com.meetr.domain.entity.Building;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.BuildingStatus;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.repository.BookingAttendeeRepository;
import com.meetr.domain.repository.BookingOperationLogRepository;
import com.meetr.domain.repository.BookingRepository;
import com.meetr.domain.repository.BuildingRepository;
import com.meetr.domain.repository.MeetingRoomRepository;
import com.meetr.domain.service.BookingRuleService;
import com.meetr.domain.service.ConflictCheckService;
import com.meetr.domain.vo.RuleViolation;
import com.meetr.domain.vo.TimeSlot;
import com.meetr.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingApplicationService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final BookingRepository bookingRepository;
    private final BookingAttendeeRepository bookingAttendeeRepository;
    private final BookingOperationLogRepository bookingOperationLogRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final BuildingRepository buildingRepository;
    private final RoomConfigApplicationService roomConfigApplicationService;
    private final ConflictCheckService conflictCheckService;
    private final BookingRuleService bookingRuleService;

    @Transactional
    public BookingResult create(CreateBookingCommand cmd) {
        MeetingRoom room = requireAvailableRoom(cmd.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(cmd.getStartTime(), cmd.getEndTime()), config);

        Booking booking = new Booking();
        booking.setRoomId(room.getId());
        booking.setSubject(cmd.getSubject());
        booking.setBookerId(cmd.getBookerId());
        booking.setBookerName(cmd.getBookerName() == null || cmd.getBookerName().isBlank() ? cmd.getBookerId() : cmd.getBookerName());
        booking.applyTimeSlot(alignedSlot);
        booking.setAttendeeCount(cmd.getAttendeeCount());
        booking.setRemark(cmd.getRemark());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setApprovalStatus(Boolean.TRUE.equals(config.getApprovalRequired()) ? ApprovalStatus.PENDING : ApprovalStatus.NONE);

        List<RuleViolation> violations = bookingRuleService.validate(booking, room, config);
        if (!violations.isEmpty()) {
            return BookingResult.rejected(violations);
        }

        ConflictCheckService.ConflictResult conflict = conflictCheckService.hasConflict(room.getId(), alignedSlot, null);
        if (conflict.conflict()) {
            return BookingResult.conflicted(toConflictDtos(conflict.conflictingBookings()));
        }

        Booking saved = bookingRepository.save(booking);
        syncAttendees(saved.getId(), cmd.getAttendeeIds());
        saveLog(saved.getId(), "CREATE", cmd.getBookerId(), booking.getBookerName(), "创建预约");
        return BookingResult.success(toDto(saved));
    }

    @Transactional
    public BookingResult update(UpdateBookingCommand cmd) {
        Booking booking = bookingRepository.findById(cmd.getBookingId())
            .orElseThrow(() -> new BusinessException(40001, "预约不存在"));
        assertOperator(booking, cmd.getOperatorId());
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BusinessException(40002, "已取消的预约不能修改");
        }

        MeetingRoom room = requireAvailableRoom(booking.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(cmd.getStartTime(), cmd.getEndTime()), config);

        booking.updateDetails(cmd.getSubject(), alignedSlot, cmd.getAttendeeCount(), cmd.getRemark());
        booking.setApprovalStatus(Boolean.TRUE.equals(config.getApprovalRequired()) ? ApprovalStatus.PENDING : ApprovalStatus.NONE);

        List<RuleViolation> violations = bookingRuleService.validate(booking, room, config);
        if (!violations.isEmpty()) {
            return BookingResult.rejected(violations);
        }

        ConflictCheckService.ConflictResult conflict = conflictCheckService.hasConflict(room.getId(), alignedSlot, booking.getId());
        if (conflict.conflict()) {
            return BookingResult.conflicted(toConflictDtos(conflict.conflictingBookings()));
        }

        Booking saved = bookingRepository.save(booking);
        saveLog(saved.getId(), "UPDATE", cmd.getOperatorId(), cmd.getOperatorId(), "修改预约");
        return BookingResult.success(toDto(saved));
    }

    @Transactional
    public void cancel(Long bookingId, String operatorId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BusinessException(40001, "预约不存在"));
        assertOperator(booking, operatorId);
        try {
            booking.cancel();
        } catch (IllegalStateException ex) {
            throw new BusinessException(40002, "预约已取消");
        }
        bookingRepository.save(booking);
        saveLog(booking.getId(), "CANCEL", operatorId, operatorId, "取消预约");
    }

    public BookingDTO getById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "预约不存在"));
        return toDto(booking);
    }

    public Page<BookingDTO> getMyBookings(String bookerId, Pageable pageable) {
        return bookingRepository.findByBookerIdOrderByStartTimeMsDesc(bookerId, pageable)
            .map(this::toDto);
    }

    public List<BookingDTO> getTodayBookings(String bookerId) {
        // 今天的 UTC 毫秒范围
        LocalDate today = LocalDate.now(BEIJING);
        long dayStartMs = today.atStartOfDay(BEIJING).toInstant().toEpochMilli();
        long dayEndMs = today.plusDays(1).atStartOfDay(BEIJING).toInstant().toEpochMilli();
        return bookingRepository.findTodayBookings(bookerId, dayStartMs, dayEndMs).stream()
            .map(this::toDto)
            .toList();
    }

    /** 查询指定会议室指定日期的全部预约（供日历视图使用） */
    public List<BookingDTO> getBookingsByRoomAndDate(Long roomId, Long dayStartMs, Long dayEndMs) {
        return bookingRepository.findByRoomIdAndDate(roomId, dayStartMs, dayEndMs).stream()
            .map(this::toDto)
            .toList();
    }

    public ConflictCheckResponse checkConflict(ConflictCheckRequest request) {
        MeetingRoom room = requireAvailableRoom(request.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(request.getStartTime(), request.getEndTime()), config);
        ConflictCheckService.ConflictResult result = conflictCheckService.hasConflict(room.getId(), alignedSlot, request.getExcludeBookingId());
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setConflict(result.conflict());
        response.setAlignedStartTime(alignedSlot.start());
        response.setAlignedEndTime(alignedSlot.end());
        response.setConflictingBookings(toConflictDtos(result.conflictingBookings()));
        return response;
    }

    private MeetingRoom requireAvailableRoom(Long roomId) {
        MeetingRoom room = meetingRoomRepository.findById(roomId)
            .orElseThrow(() -> new BusinessException(40001, "会议室不存在"));
        if (room.getStatus() != RoomStatus.ENABLED) {
            throw new BusinessException(40002, "会议室已停用");
        }
        Building building = buildingRepository.findById(room.getBuildingId())
            .orElseThrow(() -> new BusinessException(40001, "楼栋不存在"));
        if (building.getStatus() != BuildingStatus.ACTIVE) {
            throw new BusinessException(40002, "楼栋已停用");
        }
        return room;
    }

    private void assertOperator(Booking booking, String operatorId) {
        if (!booking.getBookerId().equals(operatorId)) {
            throw new BusinessException(40301, "无权操作该预约");
        }
    }

    private void syncAttendees(Long bookingId, List<String> attendeeIds) {
        bookingAttendeeRepository.deleteByBookingId(bookingId);
        List<String> normalized = attendeeIds == null ? List.of() : attendeeIds.stream()
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList();
        List<BookingAttendee> attendees = new ArrayList<>();
        for (String attendeeId : normalized) {
            BookingAttendee attendee = new BookingAttendee();
            attendee.setBookingId(bookingId);
            attendee.setUserId(attendeeId);
            attendee.setUserName(attendeeId);
            attendees.add(attendee);
        }
        if (!attendees.isEmpty()) {
            bookingAttendeeRepository.saveAll(attendees);
        }
    }

    private void saveLog(Long bookingId, String operationType, String operatorId, String operatorName, String content) {
        BookingOperationLog log = new BookingOperationLog();
        log.setBookingId(bookingId);
        log.setOperationType(operationType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setContent(content);
        bookingOperationLogRepository.save(log);
    }

    private List<BookingConflictDTO> toConflictDtos(List<Booking> bookings) {
        return bookings.stream()
            .map(booking -> new BookingConflictDTO(
                booking.getId(),
                booking.getSubject(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getBookerName()))
            .toList();
    }

    private BookingDTO toDto(Booking booking) {
        MeetingRoom room = meetingRoomRepository.findById(booking.getRoomId()).orElse(null);
        Building building = room == null ? null : buildingRepository.findById(room.getBuildingId()).orElse(null);
        List<BookingAttendeeDTO> attendees = bookingAttendeeRepository.findByBookingIdOrderByIdAsc(booking.getId()).stream()
            .map(attendee -> new BookingAttendeeDTO(attendee.getUserId(), attendee.getUserName()))
            .toList();

        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setRoomId(booking.getRoomId());
        dto.setRoomName(room == null ? null : room.getName());
        dto.setBuildingId(room == null ? null : room.getBuildingId());
        dto.setBuildingName(building == null ? null : building.getName());
        dto.setSubject(booking.getSubject());
        dto.setBookerId(booking.getBookerId());
        dto.setBookerName(booking.getBookerName());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setAttendeeCount(booking.getAttendeeCount());
        dto.setStatus(booking.getStatus());
        dto.setApprovalStatus(booking.getApprovalStatus());
        dto.setRemark(booking.getRemark());
        dto.setVersion(booking.getVersion());
        dto.setAttendees(attendees);
        return dto;
    }
}
