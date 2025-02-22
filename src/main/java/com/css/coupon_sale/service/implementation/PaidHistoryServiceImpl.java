package com.css.coupon_sale.service.implementation;

import com.css.coupon_sale.dto.request.PayOwnerRequest;
import com.css.coupon_sale.dto.response.PaidHistoryReportResponse;
import com.css.coupon_sale.dto.response.PayOwnerResponse;
import com.css.coupon_sale.entity.BusinessEntity;
import com.css.coupon_sale.entity.PaidHistoryEntity;
import com.css.coupon_sale.repository.BusinessRepository;
import com.css.coupon_sale.repository.PaidHistoryRepository;
import com.css.coupon_sale.repository.SaleCouponRepository;
import com.css.coupon_sale.service.PaidHistoryService;
import jakarta.persistence.EntityNotFoundException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaidHistoryServiceImpl implements PaidHistoryService {

    @Autowired
    private PaidHistoryRepository paidHistoryRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private SaleCouponRepository saleCouponRepository;

    @Override
    public PayOwnerResponse payOwner(PayOwnerRequest requestDto) {
        // Retrieve the business entity
        BusinessEntity business = businessRepository.findById(requestDto.getBusinessId())
                .orElseThrow(() -> new RuntimeException("Business not found"));

        // Validate owner percentage
        if (requestDto.getDesiredPercentage() < 1 || requestDto.getDesiredPercentage() > 100) {
            throw new IllegalArgumentException("Owner percentage must be between 1 and 100");
        }

        // Check if this is the first payment
        boolean isFirstPayment = (business.getLastPaidAmount() == null || business.getLastPaidAmount() == 0);

        if (!isFirstPayment && !business.isIncomeIncreased()) {
            throw new IllegalStateException("Total income has not increased since the last payment");
        }


        Double increment = business.getTotalIncome() - business.getLastPaidAmount();
        Double adminShare = increment * requestDto.getDesiredPercentage() / 100;
        Double ownerShare = increment - adminShare;

        // Update the business entity
        business.setLastPaidAmount(business.getTotalIncome());
        business.setIncomeIncreased(false);
        // Update payment status to PAID
        if (business.isIncomeIncreased()) {
            business.setPaymentStatus("PAID");
        } else {
            business.setPaymentStatus("PENDING"); // or whatever status you want to set
        }
        businessRepository.save(business);

        // Save to payment history
        PaidHistoryEntity paymentHistory = new PaidHistoryEntity();
        paymentHistory.setBusiness(business);
        paymentHistory.setPaymentDate(LocalDateTime.now());
        paymentHistory.setPaidAmount(ownerShare);
        paymentHistory.setDesiredPercentage(requestDto.getDesiredPercentage());
        paymentHistory.setAdminProfit(adminShare);
        paidHistoryRepository.save(paymentHistory);

        // Return the response DTO
        return new PayOwnerResponse(
                business.getId(),
                ownerShare,
                adminShare,
                ownerShare,
                paymentHistory.getPaymentDate(),
                adminShare
        );
    }

    @Override
    public List<PaidHistoryEntity> getPaidHistory(int businessId) {
        return paidHistoryRepository.findByBusinessIdOrderByPaymentDateDesc(businessId);
    }

    @Override
    public byte[] generatePaidHistoryReport(Integer businessId, String reportType) throws JRException {
        try {
            // Fetch data from repository
            System.out.println("Business ID" + businessId);
            List<PaidHistoryEntity> paidHistoryData;
            if (businessId == null) {
                paidHistoryData = paidHistoryRepository.findAll(); // Fetch all records
            } else {
                paidHistoryData = paidHistoryRepository.findByBusinessIdOrderByPaymentDateDesc(businessId); // Fetch records for a specific business
            }

            if (paidHistoryData == null || paidHistoryData.isEmpty()) {
                throw new RuntimeException("No paid history data available for the given business ID");
            }

            // Convert data into a list of DTOs
            List<PaidHistoryReportResponse> reportData = paidHistoryData.stream()
                    .map(history -> {
                        // Convert LocalDateTime to Date
                        Date paymentDate = convertToDate(history.getPaymentDate());
                        return new PaidHistoryReportResponse(
                                history.getBusiness().getName(),
                                paymentDate, // Use the converted Date
                                history.getPaidAmount(),
                                history.getDesiredPercentage(),
                                history.getAdminProfit()
                        );
                    })
                    .collect(Collectors.toList());

            // Prepare the data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);

            // Prepare the parameters for the report
            Map<String, Object> parameters = new HashMap<>();
            if (businessId == null) {
                parameters.put("businessName", "All Businesses"); // Set business name to "All Businesses" when no specific business is selected
            } else {
                parameters.put("businessName", reportData.isEmpty() ? "" : reportData.get(0).getBusinessName());
            }

            // Fill the report
            JasperReport jasperReport = JasperCompileManager.compileReport(getClass().getResourceAsStream("/paid-history-report.jrxml"));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Return the report in the requested format
            if ("pdf".equalsIgnoreCase(reportType)) {
                return JasperExportManager.exportReportToPdf(jasperPrint);
            } else if ("excel".equalsIgnoreCase(reportType)) {
                return exportToExcel(jasperPrint);
            } else {
                throw new IllegalArgumentException("Unsupported report type: " + reportType);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            throw new JRException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    // Helper method to convert LocalDateTime to Date
    private Date convertToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private byte[] exportToExcel(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
        return outputStream.toByteArray();
    }
}
