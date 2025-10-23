package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.SueldoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ExcelService {
    public int contarRegistros(Path excelPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(excelPath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            return sheet.getLastRowNum(); // Excluye header
        }
    }

    public List<SueldoDTO> leerChunk(Path excelPath, int startRow, int endRow) throws IOException {
        List<SueldoDTO> sueldos = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelPath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = startRow; i <= endRow && i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    SueldoDTO sueldo = mapearFila(row);
                    if (sueldo != null) {
                        sueldos.add(sueldo);
                    }
                } catch (Exception e) {
                    log.warn("Error mapeando fila {}: {}", i, e.getMessage());
                    // Continuar con siguiente fila
                }
            }
        }

        return sueldos;
    }

    private SueldoDTO mapearFila(Row row) {
        try {
            return SueldoDTO.builder()
                    .numeroEmpleado(getCellValueAsString(row.getCell(0)))
                    .nombreCompleto(getCellValueAsString(row.getCell(1)))
                    .puesto(getCellValueAsString(row.getCell(2)))
                    .salarioBase(getCellValueAsBigDecimal(row.getCell(3)))
                    .bonos(getCellValueAsBigDecimal(row.getCell(4)))
                    .deducciones(getCellValueAsBigDecimal(row.getCell(5)))
                    .salarioNeto(getCellValueAsBigDecimal(row.getCell(6)))
                    .periodoPago(getCellValueAsString(row.getCell(7)))
                    .fechaPago(getCellValueAsDate(row.getCell(8)))
                    .build();
        } catch (Exception e) {
            log.error("Error mapeando fila: {}", e.getMessage());
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim()
                            .replace(",", "")
                            .replace("$", "");
                    return new BigDecimal(value);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) return LocalDate.now();

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        // Intentar parsear string
        String dateStr = getCellValueAsString(cell);
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public boolean validarFormato(Path excelPath) {
        try (FileInputStream fis = new FileInputStream(excelPath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null || headerRow.getLastCellNum() < 9) {
                return false;
            }

            // Validar headers esperados
            String[] expectedHeaders = {
                    "Numero Empleado", "Nombre Completo", "Puesto",
                    "Salario Base", "Bonos", "Deducciones",
                    "Salario Neto", "Periodo Pago", "Fecha Pago"
            };

            for (int i = 0; i < expectedHeaders.length; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null || !getCellValueAsString(cell)
                        .equalsIgnoreCase(expectedHeaders[i].replace(" ", ""))) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validando formato: {}", e.getMessage());
            return false;
        }
    }
}
