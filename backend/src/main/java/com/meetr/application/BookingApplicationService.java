package com.meetr.application;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.meetr.application.dto.*;
import com.meetr.common.BusinessTime;
import com.meetr.domain.UserContext;
import com.meetr.domain.entity.*;
import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.BuildingStatus;
import com.meetr.domain.enums.NotificationEventType;
import com.meetr.domain.enums.RecurrenceType;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.service.BookingRuleService;
import com.meetr.domain.service.ConflictCheckService;
import com.meetr.domain.service.RecurrenceExpander;
import com.meetr.domain.vo.RuleViolation;
import com.meetr.domain.vo.TimeSlot;
import com.meetr.exception.BusinessException;
import com.meetr.mapper.BookingAttendeeMapper;
import com.meetr.mapper.BookingMapper;
import com.meetr.mapper.BookingOperationLogMapper;
import com.meetr.mapper.BuildingMapper;
import com.meetr.mapper.MeetingRoomMapper;
import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingApplicationService {

    private final BookingMapper bookingMapper;
    private final BookingAttendeeMapper bookingAttendeeMapper;
    private final BookingOperationLogMapper bookingOperationLogMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final BuildingMapper buildingMapper;
    private final RoomConfigApplicationService roomConfigApplicationService;
    private final ConflictCheckService conflictCheckService;
    private final BookingRuleService bookingRuleService;
    private final RecurrenceExpander recurrenceExpander;
    private final AdminUserService adminUserService;
    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingResult create(CreateBookingCommand cmd) {
        MeetingRoom room = requireAvailableRoom(cmd.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());

        LocalDateTime startUtc = BusinessTime.msToUtcLocalDateTime(cmd.getStartTime());
        LocalDateTime endUtc = BusinessTime.msToUtcLocalDateTime(cmd.getEndTime());
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
        publishEvent(NotificationEventType.BOOKING_CREATED, master, cmd.getBookerId(), cmd.getAttendeeIds());
        notifyApproversIfNeeded(master, config);

        RecurrenceType recType = cmd.getRecurrenceType() != null ? cmd.getRecurrenceType() : RecurrenceType.NONE;
        if (recType != RecurrenceType.NONE && cmd.getRecurrenceEndDate() != null) {
            List<long[]> instances = recurrenceExpander.expand(alignedSlot.start(), alignedSlot.end(), recType, cmd.getRecurrenceEndDate());
            for (int i = 0; i < instances.size(); i++) {
                long[] inst = instances.get(i);
                LocalDateTime s = BusinessTime.msToUtcLocalDateTime(inst[0]);
                LocalDateTime e = BusinessTime.msToUtcLocalDateTime(inst[1]);
                TimeSlot instSlot = conflictCheckService.alignToSlot(new TimeSlot(s, e), config);

                Booking child = buildBooking(cmd, room, instSlot, config, master.getId(), i + 2);
                List<RuleViolation> childViolations = bookingRuleService.validate(child, room, config);
                if (!childViolations.isEmpty()) {
                    continue;
                }

                ConflictCheckService.ConflictResult childConflict = conflictCheckService.hasConflict(room.getId(), instSlot, null);
                if (childConflict.conflict()) {
                    continue;
                }

                persistBooking(child);
                syncAttendees(child.getId(), cmd.getAttendeeIds());
                publishEvent(NotificationEventType.BOOKING_CREATED, child, cmd.getBookerId(), cmd.getAttendeeIds());
                notifyApproversIfNeeded(child, config);
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
        booking.setApprovalStatus(resolveApprovalStatus(config));
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
            throw new BusinessException(40002, "已取消预约不可修改");
        }
        if (booking.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new BusinessException(40002, "已驳回预约不可修改");
        }

        MeetingRoom room = requireAvailableRoom(booking.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());

        LocalDateTime startUtc = BusinessTime.msToUtcLocalDateTime(cmd.getStartTime());
        LocalDateTime endUtc = BusinessTime.msToUtcLocalDateTime(cmd.getEndTime());
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(startUtc, endUtc), config);

        booking.updateDetails(cmd.getSubject(), alignedSlot, cmd.getAttendeeCount(), cmd.getRemark());
        booking.setApprovalStatus(resolveApprovalStatus(config));

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
        publishEvent(NotificationEventType.BOOKING_UPDATED, booking, booking.getBookerId(), bookingAttendeeMapper.findByBookingIdOrderByIdAsc(booking.getId()).stream().map(BookingAttendee::getUserId).toList());
        return BookingResult.success(toDto(booking));
    }

    @Transactional
    public void cancel(Long bookingId, String operatorId, boolean cancelSeries) {
        Booking booking = requireBooking(bookingId);
        assertOperator(booking, operatorId);
        if (!cancelSeries) {
            booking.cancel();
            updateBooking(booking);
            saveLog(booking.getId(), "CANCEL", operatorId, operatorId, "取消预约");
            publishEvent(NotificationEventType.BOOKING_CANCELED, booking, booking.getBookerId(), null);
            return;
        }

        booking.cancel();
        updateBooking(booking);
        saveLog(booking.getId(), "CANCEL", operatorId, operatorId,
            booking.getRecurrenceType() != null && booking.getRecurrenceType() != RecurrenceType.NONE ? "取消系列预约" : "取消预约");
        publishEvent(NotificationEventType.BOOKING_CANCELED, booking, booking.getBookerId(), null);

        if (booking.getRecurrenceType() != null && booking.getRecurrenceType() != RecurrenceType.NONE) {
            if (booking.getParentId() != null) {
                List<Booking> siblings = bookingMapper.findByParentIdOrderBySeriesIndexAsc(booking.getParentId());
                for (Booking sibling : siblings) {
                    if (sibling.getId().equals(booking.getId()) || sibling.getStatus() == BookingStatus.CANCELED) {
                        continue;
                    }
                    sibling.cancel();
                    updateBooking(sibling);
                    saveLog(sibling.getId(), "CANCEL", operatorId, operatorId, "取消系列预约");
                    publishEvent(NotificationEventType.BOOKING_CANCELED, sibling, sibling.getBookerId(), null);
                }
            } else {
                List<Booking> children = bookingMapper.findByParentIdOrderBySeriesIndexAsc(booking.getId());
                for (Booking child : children) {
                    if (child.getStatus() == BookingStatus.CANCELED) {
                        continue;
                    }
                    child.cancel();
                    updateBooking(child);
                    saveLog(child.getId(), "CANCEL", operatorId, operatorId, "取消系列预约");
                    publishEvent(NotificationEventType.BOOKING_CANCELED, child, child.getBookerId(), null);
                }
            }
        }
    }

    public BookingDTO getById(Long id) {
        return toDto(requireBooking(id));
    }

    public BookingDetailDTO getDetail(Long id) {
        Booking booking = requireBooking(id);
        BookingDetailDTO detail = new BookingDetailDTO();
        detail.setBooking(toDto(booking));
        detail.setOperationLogs(bookingOperationLogMapper.findByBookingId(id).stream().map(log -> {
            BookingOperationLogDTO dto = new BookingOperationLogDTO();
            dto.setId(log.getId());
            dto.setBookingId(log.getBookingId());
            dto.setOperationType(log.getOperationType());
            dto.setOperatorId(log.getOperatorId());
            dto.setOperatorName(log.getOperatorName());
            dto.setContent(log.getContent());
            dto.setCreatedAtMs(log.getCreatedAtMs());
            return dto;
        }).toList());
        return detail;
    }

    public PageResult<BookingDTO> getMyBookings(String bookerId, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Booking> bookings = bookingMapper.findByBookerIdOrderByStartTimeMsDesc(bookerId);
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        List<BookingDTO> dtos = bookings.stream().map(this::toDto).toList();
        return new PageResult<>(dtos, pageInfo.getTotal());
    }

    public List<BookingDTO> getTodayBookings(String bookerId) {
        LocalDate today = LocalDate.now(BusinessTime.BUSINESS_ZONE);
        long dayStartMs = BusinessTime.dayStartMs(today);
        long dayEndMs = BusinessTime.dayEndMs(today);
        return bookingMapper.findTodayBookings(bookerId, dayStartMs, dayEndMs).stream()
            .map(this::toDto)
            .toList();
    }

    public PageResult<BookingDTO> searchBookings(BookingSearchRequest request) {
        PageHelper.startPage(request.getPage() + 1, request.getSize());
        List<Booking> bookings = bookingMapper.searchBookings(
            request.getBookerId(), request.getKeyword(), request.getRoomId(), request.getStatus(),
            request.getStartTimeFrom(), request.getStartTimeTo());
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        List<BookingDTO> dtos = bookings.stream().map(this::toDto).toList();
        return new PageResult<>(dtos, pageInfo.getTotal());
    }

    public PageResult<BookingDTO> getPendingBookings(PendingBookingQuery query) {
        PageHelper.startPage(query.getPage() + 1, query.getSize());
        List<Booking> bookings = bookingMapper.findPendingBookings(
            query.getBuildingId(), query.getRoomId(), normalize(query.getBookerId()), normalize(query.getKeyword()),
            query.getStartDateMs(), query.getEndDateMs());
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        List<BookingDTO> dtos = bookings.stream().map(this::toDto).toList();
        return new PageResult<>(dtos, pageInfo.getTotal());
    }

    @Transactional
    public BookingDTO approveBooking(Long bookingId, String operatorId, String remark) {
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        Booking booking = requireBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BusinessException(40002, "已取消预约不可审批");
        }
        if (booking.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(40002, "当前预约不是待审批状态");
        }

        booking.setApprovalStatus(ApprovalStatus.APPROVED);
        updateBooking(booking);
        String operatorName = resolveOperatorName(operatorId);
        String content = (remark == null || remark.isBlank()) ? "审批通过" : "审批通过：" + remark.trim();
        saveLog(booking.getId(), "APPROVE", operatorId, operatorName, content);
        publishEvent(NotificationEventType.BOOKING_APPROVED, booking, booking.getBookerId(), null);
        return toDto(booking);
    }

    @Transactional
    public BookingDTO rejectBooking(Long bookingId, String operatorId, String remark) {
        Objects.requireNonNull(operatorId, "operatorId must not be null");
        Booking booking = requireBooking(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BusinessException(40002, "已取消预约不可审批");
        }
        if (booking.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(40002, "当前预约不是待审批状态");
        }

        booking.setApprovalStatus(ApprovalStatus.REJECTED);
        updateBooking(booking);
        String operatorName = resolveOperatorName(operatorId);
        String content = (remark == null || remark.isBlank()) ? "审批驳回" : "审批驳回：" + remark.trim();
        saveLog(booking.getId(), "REJECT", operatorId, operatorName, content);
        publishEvent(NotificationEventType.BOOKING_REJECTED, booking, booking.getBookerId(), null);
        return toDto(booking);
    }

    public List<BookingDTO> getBookingsByRoomAndDate(Long roomId, Long dayStartMs, Long dayEndMs) {
        return bookingMapper.findByRoomIdAndDate(roomId, dayStartMs, dayEndMs).stream()
            .map(this::toDto)
            .toList();
    }

    public ConflictCheckResponse checkConflict(ConflictCheckRequest request) {
        MeetingRoom room = requireAvailableRoom(request.getRoomId());
        RoomConfig config = roomConfigApplicationService.getEnabledEffectiveConfigEntity(room.getId());
        LocalDateTime startUtc = BusinessTime.msToUtcLocalDateTime(request.getStartTime());
        LocalDateTime endUtc = BusinessTime.msToUtcLocalDateTime(request.getEndTime());
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(new TimeSlot(startUtc, endUtc), config);
        ConflictCheckService.ConflictResult result = conflictCheckService.hasConflict(room.getId(), alignedSlot, request.getExcludeBookingId());

        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setConflict(result.conflict());
        response.setAlignedStartTime(BusinessTime.utcLocalDateTimeToMs(alignedSlot.start()));
        response.setAlignedEndTime(BusinessTime.utcLocalDateTimeToMs(alignedSlot.end()));
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

    private ApprovalStatus resolveApprovalStatus(RoomConfig config) {
        if (!Boolean.TRUE.equals(config.getApprovalRequired())) {
            return ApprovalStatus.NONE;
        }
        return UserContext.isAdmin() ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

        var logs = bookingOperationLogMapper.findByBookingId(booking.getId());
        for (int i = logs.size() - 1; i >= 0; i--) {
            var log = logs.get(i);
            if ("APPROVE".equals(log.getOperationType()) || "REJECT".equals(log.getOperationType())) {
                dto.setLastApprovalOperatorId(log.getOperatorId());
                dto.setLastApprovalOperatorName(log.getOperatorName());
                dto.setLastApprovalAtMs(log.getCreatedAtMs());
                break;
            }
        }
        return dto;
    }

    /**
     * 获取某个预约所属系列的所有预约（包含主预约）。
     * 传入 bookingId 可以是任意一次（主预约或子预约）。
     */
    public SeriesBookingResponse getSeriesBookings(Long bookingId, String bookerId) {
        Booking seed = bookingMapper.findById(bookingId);
        if (seed == null) {
            throw new BusinessException(40001, "预约不存在");
        }
        if (!seed.getBookerId().equals(bookerId)) {
            throw new BusinessException(40301, "无权查看该预约");
        }

        List<Booking> all = bookingMapper.findSeriesBookings(bookingId, bookerId);
        if (all.isEmpty()) {
            throw new BusinessException(40001, "系列不存在");
        }

        Booking master = all.get(0);
        List<BookingDTO> instances = all.stream()
            .skip(1) // 第一个是 master
            .map(this::toDto)
            .toList();

        SeriesBookingResponse resp = new SeriesBookingResponse();
        resp.setMaster(toDto(master));
        resp.setInstances(instances);
        resp.setTotalCount(all.size());
        return resp;
    }

    /**
     * 批量修改系列中从 fromSeriesIndex 开始的所有未来预约的时间。
     * 计算 master 时间偏移量，施加到每个子预约上，保持各自的日期不变。
     * 不改变其他字段，不做冲突检测（系列内部时间本身不应冲突）。
     */
    @Transactional
    public SeriesBookingResponse updateFutureSeries(Long bookingId, String operatorId,
                                                   UpdateFutureSeriesRequest req) {
        SeriesBookingResponse series = getSeriesBookings(bookingId, operatorId);
        Booking masterEntity = bookingMapper.findById(series.getMaster().getId());

        // 时间偏移量
        long offsetMs = req.getNewStartTimeMs() - masterEntity.getStartTimeMs();
        long newEndMs = req.getNewEndTimeMs();

        List<Booking> allForLog = bookingMapper.findSeriesBookings(bookingId, operatorId);
        for (Booking child : allForLog) {
            if (child.getSeriesIndex() < req.getFromSeriesIndex()) {
                continue; // 跳过主预约和修改范围之前的
            }
            if (child.getStatus() == com.meetr.domain.enums.BookingStatus.CANCELED) {
                continue;
            }

            // 新时间 = 原时间 + 偏移量，保持各自的日期
            long shiftedStart = child.getStartTimeMs() + offsetMs;
            long shiftedEnd   = child.getEndTimeMs() + offsetMs;

            child.setStartTimeMs(shiftedStart);
            child.setEndTimeMs(shiftedEnd);
            bookingMapper.update(child);
        }

        saveLog(masterEntity.getId(), "UPDATE_FUTURE", operatorId, operatorId,
            "修改系列第" + req.getFromSeriesIndex() + "场及后续预约的时间");

        return getSeriesBookings(bookingId, operatorId);
    }

    private void publishEvent(NotificationEventType eventType, Booking booking,
                               String bookerId, List<String> attendeeIds) {
        List<String> targetUserIds = new java.util.ArrayList<>();
        if (bookerId != null && !bookerId.isBlank()) {
            targetUserIds.add(bookerId);
        }
        if (attendeeIds != null) {
            targetUserIds.addAll(attendeeIds);
        }
        targetUserIds = targetUserIds.stream()
            .filter(userId -> userId != null && !userId.isBlank())
            .distinct()
            .toList();
        eventPublisher.publishEvent(new com.meetr.domain.event.BookingDomainEvent(
            this, eventType, booking, targetUserIds));
    }

    private void notifyApproversIfNeeded(Booking booking, RoomConfig config) {
        if (!Boolean.TRUE.equals(config.getApprovalRequired())) {
            return;
        }
        if (booking.getApprovalStatus() != ApprovalStatus.PENDING) {
            return;
        }
        List<String> adminIds = adminUserService.listAdminUserIds();
        if (adminIds == null || adminIds.isEmpty()) {
            return;
        }
        eventPublisher.publishEvent(new com.meetr.domain.event.BookingDomainEvent(
            this, NotificationEventType.BOOKING_APPROVAL_REQUIRED, booking, adminIds));
    }

    private String resolveOperatorName(String operatorId) {
        if (operatorId == null || operatorId.isBlank()) {
            return null;
        }
        try {
            return authService.getUserDetail(operatorId).name();
        } catch (Exception e) {
            return operatorId;
        }
    }

    public record PageResult<T>(List<T> content, long totalElements) {
    }
}
