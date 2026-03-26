package com.meetr.application;

import com.meetr.application.dto.BookingAttendeeDTO;
import com.meetr.application.dto.BookingConflictDTO;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.BookingResult;
import com.meetr.application.dto.BookingSearchRequest;
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
import com.meetr.domain.enums.RecurrenceType;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.repository.BookingAttendeeRepository;
import com.meetr.domain.repository.BookingOperationLogRepository;
import com.meetr.domain.repository.BookingRepository;
import com.meetr.domain.repository.BuildingRepository;
import com.meetr.domain.repository.MeetingRoomRepository;
import com.meetr.domain.service.BookingRuleService;
import com.meetr.domain.service.ConflictCheckService;
import com.meetr.domain.service.RecurrenceExpander;
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
import java.time.ZoneOffset;
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
    private final RecurrenceExpander recurrenceExpander;

    @Transactional
    public BookingResult create(CreateBookingCommand cmd) {
        MeetingRoom room = requireAvailableRoom(cmd.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());

        LocalDateTime startUtc = LocalDateTime.ofEpochSecond(cmd.getStartTime() / 1000, 0, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofEpochSecond(cmd.getEndTime() / 1000, 0, ZoneOffset.UTC);
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(startUtc, endUtc), config);

        Booking master = buildBooking(cmd, room, alignedSlot, config, null, 1);

        List<RuleViolation> violations = bookingRuleService.validate(master, room, config);
        if (!violations.isEmpty()) {
            return BookingResult.rejected(violations);
        }

        ConflictCheckService.ConflictResult conflict = conflictCheckService.hasConflict(room.getId(), alignedSlot, null);
        if (conflict.conflict()) {
            return BookingResult.conflicted(toConflictDtos(conflict.conflictingBookings()));
        }

        // 保存主预约
        Booking saved = bookingRepository.save(master);
        syncAttendees(saved.getId(), cmd.getAttendeeIds());
        saveLog(saved.getId(), "CREATE", cmd.getBookerId(), saved.getBookerName(), "创建预约");

        // 展开并生成子实例
        RecurrenceType recType = cmd.getRecurrenceType() != null ? cmd.getRecurrenceType() : RecurrenceType.NONE;
        if (recType != RecurrenceType.NONE && cmd.getRecurrenceEndDate() != null) {
            List<long[]> instances = recurrenceExpander.expand(
                alignedSlot.start(), alignedSlot.end(), recType, cmd.getRecurrenceEndDate());
            int generated = 0;
            for (int i = 0; i < instances.size(); i++) {
                long[] inst = instances.get(i);
                LocalDateTime s = LocalDateTime.ofEpochSecond(inst[0] / 1000, 0, ZoneOffset.UTC);
                LocalDateTime e = LocalDateTime.ofEpochSecond(inst[1] / 1000, 0, ZoneOffset.UTC);
                TimeSlot instSlot = conflictCheckService.alignToSlot(new TimeSlot(s, e), config);

                Booking child = buildBooking(cmd, room, instSlot, config, saved.getId(), i + 2);
                List<RuleViolation> childViolations = bookingRuleService.validate(child, room, config);
                if (!childViolations.isEmpty()) continue; // 跳过不满足规则的实例

                ConflictCheckService.ConflictResult childConflict =
                    conflictCheckService.hasConflict(room.getId(), instSlot, null);
                if (childConflict.conflict()) continue; // 跳过冲突的实例

                Booking childSaved = bookingRepository.save(child);
                syncAttendees(childSaved.getId(), cmd.getAttendeeIds());
                generated++;
            }
            // 提示主预约已含 seriesIndex=1
        }

        return BookingResult.success(toDto(saved));
    }

    private Booking buildBooking(CreateBookingCommand cmd, MeetingRoom room, TimeSlot slot,
                                 RoomConfig config, Long parentId, int seriesIndex) {
        Booking b = new Booking();
        b.setRoomId(room.getId());
        b.setSubject(cmd.getSubject());
        b.setBookerId(cmd.getBookerId());
        b.setBookerName(cmd.getBookerName() == null || cmd.getBookerName().isBlank()
            ? cmd.getBookerId() : cmd.getBookerName());
        b.applyTimeSlot(slot);
        b.setAttendeeCount(cmd.getAttendeeCount());
        b.setRemark(cmd.getRemark());
        b.setStatus(BookingStatus.BOOKED);
        b.setApprovalStatus(
            Boolean.TRUE.equals(config.getApprovalRequired()) ? ApprovalStatus.PENDING : ApprovalStatus.NONE);
        b.setRecurrenceType(cmd.getRecurrenceType() != null ? cmd.getRecurrenceType() : RecurrenceType.NONE);
        b.setRecurrenceEndDate(cmd.getRecurrenceEndDate());
        b.setParentId(parentId);
        b.setSeriesIndex(seriesIndex);
        return b;
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
        LocalDateTime startUtc = LocalDateTime.ofEpochSecond(cmd.getStartTime() / 1000, 0, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofEpochSecond(cmd.getEndTime() / 1000, 0, ZoneOffset.UTC);
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(startUtc, endUtc), config);

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
    public void cancel(Long bookingId, String operatorId, boolean cancelSeries) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BusinessException(40001, "预约不存在"));
        assertOperator(booking, operatorId);
        try {
            booking.cancel();
        } catch (IllegalStateException ex) {
            throw new BusinessException(40002, "预约已取消");
        }
        bookingRepository.save(booking);
        saveLog(booking.getId(), "CANCEL", operatorId, operatorId,
            cancelSeries ? "取消全系列预约" : "取消预约");

        // 取消整个系列（同时取消所有子实例）
        if (cancelSeries) {
            if (booking.getParentId() != null) {
                // 是子实例 → 取消同系列所有子实例
                List<Booking> siblings = bookingRepository.findByParentIdOrderBySeriesIndexAsc(booking.getParentId());
                for (Booking sibling : siblings) {
                    if (sibling.getStatus() != BookingStatus.CANCELED) {
                        sibling.cancel();
                        bookingRepository.save(sibling);
                        saveLog(sibling.getId(), "CANCEL", operatorId, operatorId, "取消系列子预约");
                    }
                }
            } else {
                // 是主预约 → 取消所有子实例
                List<Booking> children = bookingRepository.findByParentIdOrderBySeriesIndexAsc(booking.getId());
                for (Booking child : children) {
                    if (child.getStatus() != BookingStatus.CANCELED) {
                        child.cancel();
                        bookingRepository.save(child);
                        saveLog(child.getId(), "CANCEL", operatorId, operatorId, "取消系列子预约");
                    }
                }
            }
        }
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

    public Page<BookingDTO> searchBookings(BookingSearchRequest req) {
        return bookingRepository.searchBookings(
            req.getBookerId(),
            req.getKeyword(),
            req.getRoomId(),
            req.getStatus(),
            req.getStartTimeFrom(),
            req.getStartTimeTo(),
            org.springframework.data.domain.PageRequest.of(req.getPage(), req.getSize())
        ).map(this::toDto);
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
        LocalDateTime startUtc = LocalDateTime.ofEpochSecond(request.getStartTime() / 1000, 0, ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofEpochSecond(request.getEndTime() / 1000, 0, ZoneOffset.UTC);
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(startUtc, endUtc), config);
        ConflictCheckService.ConflictResult result = conflictCheckService.hasConflict(room.getId(), alignedSlot, request.getExcludeBookingId());
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setConflict(result.conflict());
        response.setAlignedStartTime(alignedSlot.start().toInstant(ZoneOffset.UTC).toEpochMilli());
        response.setAlignedEndTime(alignedSlot.end().toInstant(ZoneOffset.UTC).toEpochMilli());
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
                booking.getStartTimeMs(),
                booking.getEndTimeMs(),
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
        dto.setStartTime(booking.getStartTimeMs());
        dto.setEndTime(booking.getEndTimeMs());
        dto.setAttendeeCount(booking.getAttendeeCount());
        dto.setStatus(booking.getStatus());
        dto.setApprovalStatus(booking.getApprovalStatus());
        dto.setRemark(booking.getRemark());
        dto.setVersion(booking.getVersion());
        dto.setAttendees(attendees);
        dto.setRecurrenceType(booking.getRecurrenceType());
        dto.setRecurrenceEndDate(booking.getRecurrenceEndDate());
        dto.setParentId(booking.getParentId());
        dto.setSeriesIndex(booking.getSeriesIndex());
        return dto;
    }
}
