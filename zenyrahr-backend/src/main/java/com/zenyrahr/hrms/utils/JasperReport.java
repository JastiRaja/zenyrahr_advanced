package com.zenyrahr.hrms.utils;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;
import org.springframework.stereotype.Component;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Document;

import java.io.ByteArrayOutputStream;

@Component
public class JasperReport {

    public ByteArrayOutputStream exportToPdfStream(JasperPrint jasperPrint) throws JRException {
        // Create a ByteArrayOutputStream to hold the output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Create PDF exporter
        JRPdfExporter exporter = new JRPdfExporter();

        // Set the JasperPrint object
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

        // Set the output stream
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        // Configure PDF encryption and permissions (optional)
        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setEncrypted(true);
        config.set128BitKey(true); // 128-bit encryption
//        config.setOwnerPassword("secure-password"); // Password for editing permissions
//        config.setUserPassword("123");  // Password for viewing
        config.setPermissions(PdfWriter.ALLOW_PRINTING); // Only allow printing, no modification
        exporter.setConfiguration(config);

        // Export the report to the output stream
        exporter.exportReport();

        return outputStream;
    }
}
