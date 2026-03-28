package com.meetr.controller;

import com.meetr.application.BookingReportService;
import com.meetr.config.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final BookingReportService reportService;

    @RequirePermission("booking:view")
    @GetMapping("/room-usage")
    public ApiResponse<?> roomUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId) {
        return ApiResponse.ok(reportService.roomUsage(startDate, endDate, buildingId));
    }

    @RequirePermission("booking:view")
    @GetMapping("/booking-records")
    public ApiResponse<?> bookingRecords(
            @RequestParam(required = false) String buildingIds,
            @RequestParam(required = false) String roomIds,
            @RequestParam(required = false) String bookerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long startFromMs,
            @RequestParam(required = false) Long startToMs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.bookingRecords(
            buildingIds, roomIds, bookerId, keyword, status, approvalStatus,
            startFromMs, startToMs, page, size));
    }

    @RequirePermission("booking:view")
    @GetMapping("/booking-records/export")
    public void exportBookingRecords(
            @RequestParam(required = false) String bookerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long startFromMs,
            @RequestParam(required = false) Long startToMs,
            HttpServletResponse response) throws Exception {
        List<Map<String, Object>> records = reportService.exportBookingRecords(
            bookerId, keyword, status, approvalStatus, startFromMs, startToMs);

        if (records.isEmpty()) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("没有数据");
            return;
        }

        String filename = "预约记录_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (SXSSFWorkbook wb = new SXSSFWorkbook(records.size() + 100)) {
            wb.setCompressTempFiles(true);
            Sheet sheet = wb.createSheet("预约记录");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            BorderStyle thin = BorderStyle.THIN;
            headerStyle.setBorderTop(thin);
            headerStyle.setBorderBottom(thin);
            headerStyle.setBorderLeft(thin);
            headerStyle.setBorderRight(thin);

            Map<String, Object> first = records.get(0);
            String[] headers = first.keySet().toArray(new String[0]);

            // 表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 数据行
            for (int ri = 0; ri < records.size(); ri++) {
                Row row = sheet.createRow(ri + 1);
                Map<String, Object> rec = records.get(ri);
                for (int ci = 0; ci < headers.length; ci++) {
                    Cell cell = row.createCell(ci);
                    Object val = rec.get(headers[ci]);
                    if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else {
                        cell.setCellValue(val != null ? val.toString() : "");
                    }
                }
            }

            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    @RequirePermission("booking:view")
    @GetMapping("/user-usage")
    public ApiResponse<?> userUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(reportService.userUsage(startDate, endDate));
    }
}
