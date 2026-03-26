package com.meetr.application;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.meetr.application.dto.*;
import com.meetr.domain.entity.*;
import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.BuildingStatus;
import com.meetr.domain.enums.RecurrenceType;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.service.BookingRuleService;
import com.meetr.domain.service.ConflictCheckService;
import com.meetr.domain.service.RecurrenceExpander;
import com.meetr.domain.vo.RuleViolation;
import com.meetr.domain.vo.TimeSlot;
import com.meetr.exception.BusinessException;
import com.meetr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingApplicationService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final BookingMapper bookingMapper;
    private final BookingAttendeeMapper bookingAttendeeMapper;
    private final BookingOperationLogMapper bookingOperationLogMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final BuildingMapper buildingMapper;
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

        persistBooking(master);

        syncAttendees(master.getId(), cmd.getAttendeeIds());
        saveLog(master.getId(), "CREATE", cmd.getBookerId(), master.getBookerName(), "创建预约");

        RecurrenceType recType = cmd.getRecurrenceType() != null ? cmd.getRecurrenceType() : RecurrenceType.NONE;
        if (recType != RecurrenceType.NONE && cmd.getRecurrenceEndDate() != null) {
            List<long[]> instances = recurrenceExpander.expand(
                alignedSlot.start(), alignedSlot.end(), recType, cmd.getRecurrenceEndDate());
            for (int i = 0; i < instances.size(); i++) {
                long[] inst = instances.get(i);
                LocalDateTime s = LocalDateTime.ofEpochSecond(inst[0] / 1000, 0, ZoneOffset.UTC);
                LocalDateTime e = LocalDateTime.ofEpochSecond(inst[1] / 1000, 0, ZoneOffset.UTC);
                TimeSlot instSlot = conflictCheckService.alignToSlot(new TimeSlot(s, e), config);

                Booking child = buildBooking(cmd, room, instSlot, config, master.getId(), i + 2);
                List<RuleViolation> childViolations = bookingRuleService.validate(child, room, config);
                if (!childViolations.isEmpty()) continue;

                ConflictCheckService.ConflictResult childConflict =
                    conflictCheckService.hasConflict(room.getId(), instSlot, null);
                if (childConflict.conflict()) continue;

                persistBooking(child);
                syncAttendees(child.getId(), cmd.getAttendeeIds());
            }
        }

        return BookingResult.success(toDto(master));
    }

    private Booking buildBooking(CreateBookingCommand cmd, MeetingRoom room, TimeSlot slot,
                                 RoomConfig config, Long parentId, int seriesIndex) {
        Booking booking = new Booking();
        booking.setRoomId(room.getId());
        booking.setSubject(cmd.getSubject());
        booking.setBookerId(cmd.getBookerId());
        booking.setBookerName(cmd.getBookerName() == null || cmd.getBookerName().isBlank() ? cmd.getBookerId() : cmd.getBookerName());
        booking.applyTimeSlot(slot);
        booking.setAttendeeCount(cmd.getAttendeeCount());
        booking.setRemark(cmd.getRemark());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setApprovalStatus(Boolean.TRUE.equals(config.getApprovalRequired()) ? ApprovalStatus.PENDING : ApprovalStatus.NONE);
        booking.setRecurrenceType(cmd.getRecurrenceType() != null ? cmd.getRecurrenceType() : RecurrenceType.NONE);
        booking.setRecurrenceEndDate(cmd.getRecurrenceEndDate());
        booking.setParentId(parentId);
        booking.setSeriesIndex(seriesIndex);
        booking.setVersion(0L);
        return booking;
    }

    @Transactional
    public BookingResult update(UpdateBookingCommand cmd) {
        Booking booking = requireBooking(cmd.getBookingId());
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

        updateBooking(booking);
        saveLog(booking.getId(), "UPDATE", cmd.getOperatorId(), cmd.getOperatorId(), "修改预约");
        return BookingResult.success(toDto(booking));
    }

    @Transactional
    public void cancel(Long bookingId, String operatorId, boolean cancelSeries) {
        Booking booking = requireBooking(bookingId);
        assertOperator(booking, operatorId);
        try {
            booking.cancel();
        } catch (IllegalStateException ex) {
            throw new BusinessException(40002, "预约已取消");
        }
        updateBooking(booking);
        saveLog(booking.getId(), "CANCEL", operatorId, operatorId,
            cancelSeries ? "取消全系列预约" : "取消预约");

        if (cancelSeries) {
            if (booking.getParentId() != null) {
                List<Booking> siblings = bookingMapper.findByParentIdOrderBySeriesIndexAsc(booking.getParentId());
                for (Booking sibling : siblings) {
                    if (sibling.getStatus() != BookingStatus.CANCELED) {
                        sibling.cancel();
                        updateBooking(sibling);
                        saveLog(sibling.getId(), "CANCEL", operatorId, operatorId, "取消系列子预约");
                    }
                }
            } else {
                List<Booking> children = bookingMapper.findByParentIdOrderBySeriesIndexAsc(booking.getId());
                for (Booking child : children) {
                    if (child.getStatus() != BookingStatus.CANCELED) {
                        child.cancel();
                        updateBooking(child);
                        saveLog(child.getId(), "CANCEL", operatorId, operatorId, "取消系列子预约");
                    }
                }
            }
        }
    }

    public BookingDTO getById(Long id) {
        return toDto(requireBooking(id));
    }

    public PageResult<BookingDTO> getMyBookings(String bookerId, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Booking> bookings = bookingMapper.findByBookerIdOrderByStartTimeMsDesc(bookerId);
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        List<BookingDTO> dtos = bookings.stream().map(this::toDto).toList();
        return new PageResult<>(dtos, pageInfo.getTotal(), page, size);
    }

    public List<BookingDTO> getTodayBookings(String bookerId) {
        LocalDate today = LocalDate.now(BEIJING);
        long dayStartMs = today.atStartOfDay(BEIJING).toInstant().toEpochMilli();
        long dayEndMs = today.plusDays(1).atStartOfDay(BEIJING).toInstant().toEpochMilli();
        return bookingMapper.findTodayBookings(bookerId, dayStartMs, dayEndMs).stream()
            .map(this::toDto)
            .toList();
    }

    public PageResult<BookingDTO> searchBookings(BookingSearchRequest req) {
        PageHelper.startPage(req.getPage() + 1, req.getSize());
        List<Booking> bookings = bookingMapper.searchBookings(
            req.getBookerId(), req.getKeyword(), req.getRoomId(),
            req.getStatus(), req.getStartTimeFrom(), req.getStartTimeTo());
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        List<BookingDTO> dtos = bookings.stream().map(this::toDto).toList();
        return new PageResult<>(dtos, pageInfo.getTotal(), req.getPage(), req.getSize());
    }

    public List<BookingDTO> getBookingsByRoomAndDate(Long roomId, Long dayStartMs, Long dayEndMs) {
        return bookingMapper.findByRoomIdAndDate(roomId, dayStartMs, dayEndMs).stream()
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

    private Booking requireBooking(Long id) {
        Booking booking = bookingMapper.findById(id);
        if (booking == null) {
            throw new BusinessException(40001, "预约不存在");
        }
        return booking;
    }

    private MeetingRoom requireAvailableRoom(Long roomId) {
        MeetingRoom room = meetingRoomMapper.findById(roomId);
        if (room == null) {
            throw new BusinessException(40001, "会议室不存在");
        }
        if (room.getStatus() != RoomStatus.ENABLED) {
            throw new BusinessException(40002, "会议室已停用");
        }
        Building building = buildingMapper.findById(room.getBuildingId());
        if (building == null) {
            throw new BusinessException(40001, "楼栋不存在");
        }
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

    private void persistBooking(Booking booking) {
        booking.initTimestampsForInsert();
        bookingMapper.insert(booking);
    }

    private void updateBooking(Booking booking) {
        booking.touchForUpdate();
        booking.setVersion(booking.getVersion() == null ? 1L : booking.getVersion() + 1);
        bookingMapper.update(booking);
    }

    private void syncAttendees(Long bookingId, List<String> attendeeIds) {
        bookingAttendeeMapper.deleteByBookingId(bookingId);
        List<String> normalized = attendeeIds == null ? List.of() : attendeeIds.stream()
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList();
        for (String attendeeId : normalized) {
            BookingAttendee attendee = new BookingAttendee();
            attendee.setBookingId(bookingId);
            attendee.setUserId(attendeeId);
            attendee.setUserName(attendeeId);
            attendee.initTimestampsForInsert();
            bookingAttendeeMapper.insert(attendee);
        }
    }

    private void saveLog(Long bookingId, String operationType, String operatorId, String operatorName, String content) {
        BookingOperationLog logEntry = new BookingOperationLog();
        logEntry.setBookingId(bookingId);
        logEntry.setOperationType(operationType);
        logEntry.setOperatorId(operatorId);
        logEntry.setOperatorName(operatorName);
        logEntry.setContent(content);
        logEntry.initTimestampsForInsert();
        bookingOperationLogMapper.insert(logEntry);
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
        MeetingRoom room = meetingRoomMapper.findById(booking.getRoomId());
        Building building = room == null ? null : buildingMapper.findById(room.getBuildingId());
        List<BookingAttendeeDTO> attendees = bookingAttendeeMapper.findByBookingIdOrderByIdAsc(booking.getId()).stream()
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

    /** 通用分页结果 */
    public record PageResult<T>(List<T> content, long total, int page, int size) {}
}
