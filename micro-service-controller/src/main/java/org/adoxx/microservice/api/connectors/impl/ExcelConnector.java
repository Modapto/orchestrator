package org.adoxx.microservice.api.connectors.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.adoxx.microservice.api.connectors.SyncConnectorA;
import org.adoxx.microservice.utils.Utils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelConnector extends SyncConnectorA{

    @Override
    public String getName() {
        return "Excel Sheet Connector";
    }

    @Override
    public JsonObject getDescription() {
        return Json.createObjectBuilder()
            .add("en", "Get data from an Excel sheet")
            .add("de", "Get data from an Excel sheet")
            .build();
    }

    @Override
    public JsonObject getStartConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("filePath", Json.createObjectBuilder()
                .add("name", "File Path")
                .add("description", Json.createObjectBuilder()
                    .add("en", "URL/Path/Base64 of the Excel file")
                    .add("de", "URL/Path/Base64 of the Excel file"))
                .add("value", "")
                .add("moreInfos", Json.createObjectBuilder()
                    .add("requireUpload", true)))
            .add("password", Json.createObjectBuilder()
                .add("name", "Password")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Excel file password (OPTIONAL)")
                    .add("de", "Excel file password (OPTIONAL)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public JsonObject getCallConfigurationTemplate() {
        return Json.createObjectBuilder()
            .add("sheetNumber", Json.createObjectBuilder()
                .add("name", "Sheet Number")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Sheet Number")
                    .add("de", "Sheet Number"))
                .add("value", ""))
            .add("cellSeries", Json.createObjectBuilder()
                .add("name", "Cell Series")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Require a comma separated list or rage (using '-') of columns/rows where the series are defined (ex: B,E-C or 0-3 or 0,1,2,3)")
                    .add("de", "Require a comma separated list or rage (using '-') of columns/rows where the series are defined (ex: B,E-C or 0-3 or 0,1,2,3)"))
                .add("value", ""))
            .add("cellSeriesName", Json.createObjectBuilder()
                .add("name", "Cell Series Name")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Require the column/row where the name of the serie can be found or alternatively a list of names in JSON Array format (Ex: 2 or A or [\"Values\", \"Instants\"])")
                    .add("de", "Require the column/row where the name of the serie can be found or alternatively a list of names in JSON Array format (Ex: 2 or A or [\"Values\", \"Instants\"])"))
                .add("value", ""))
            .add("cellValues", Json.createObjectBuilder()
                .add("name", "Cell Values")
                .add("description", Json.createObjectBuilder()
                    .add("en", "Comma separated list or rage (using '-') of columns/rows that represent the set of cells that contain the data of the series (Ex: 3-10 or B-D,E or F-B or 10-3)")
                    .add("de", "Comma separated list or rage (using '-') of columns/rows that represent the set of cells that contain the data of the series (Ex: 3-10 or B-D,E or F-B or 10-3)"))
                .add("value", ""))
            .build();
    }
    
    @Override
    public String getOutputDescription() throws Exception {
        return "A JSON object in the following format:\n"
                + "{" + "\n"
                + "  columns : ['_cell_series_column_name_1', '_cell_series_column_name_2', ...]," + "\n"
                + "  data : [{" + "\n"
                + "    _cell_series_column_name_1 : 'cell_value_1_for_column_1'," + "\n"
                + "    _cell_series_column_name_2 : 'cell_value_1_for_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }, {" + "\n"
                + "    _cell_series_column_name_1 : 'cell_value_2_for_column_1'," + "\n"
                + "    _cell_series_column_name_2 : 'cell_value_2_for_column_2'," + "\n"
                + "    ..." + "\n"
                + "  }]," + "\n"
                + "  moreInfo : {" + "\n"
                + "    retrievalTime : ''" + "\n"
                + "  }" + "\n"
                + "}" + "\n"
                ;
    }
    
    private String filePath, password = "";
    //private InputStream input = null;
    //private Workbook workbook = null;
    
    @Override
    public void start(JsonObject startConfiguration) throws Exception {
        filePath = startConfiguration.getJsonObject("filePath")==null?"":startConfiguration.getJsonObject("filePath").getString("value", "");
        if(filePath.isEmpty()) throw new Exception("Excel file path not provided");
        password = startConfiguration.getJsonObject("password")==null?"":startConfiguration.getJsonObject("password").getString("value", "");
        //input = filePath.startsWith("http")?new URL(filePath).openConnection().getInputStream():new FileInputStream(new File(Utils.revealLocalFile(filePath)));
        //workbook = WorkbookFactory.create(input, password!=""?password:null);
    }
    
    @Override
    public JsonObject performCall(JsonObject configuration) throws Exception {
        Integer sheetNumber = -1;
        try {
            sheetNumber = Integer.parseInt(configuration.getJsonObject("sheetNumber")==null?"-1":configuration.getJsonObject("sheetNumber").getString("value", "-1"));
        }catch(Exception ex) {
            throw new Exception("The Excel sheet page number is incorrect: '"+configuration.getJsonObject("sheetNumber").getString("value")+"'");
        }
        if(sheetNumber < 1) throw new Exception("The Excel sheet page number must be greater then 0");
        String cellSeries = configuration.getJsonObject("cellSeries")==null?"":configuration.getJsonObject("cellSeries").getString("value", "").replace(" ", "").toUpperCase();
        if(cellSeries.isEmpty()) throw new Exception("Excel cell series not provided");
        String cellSeriesName = configuration.getJsonObject("cellSeriesName")==null?"":configuration.getJsonObject("cellSeriesName").getString("value", "").trim();
        String cellValues = configuration.getJsonObject("cellValues")==null?"":configuration.getJsonObject("cellValues").getString("value", "").replace(" ", "").toUpperCase();
        if(cellValues.isEmpty()) throw new Exception("Excel cell values not provided");
        
        InputStream input = filePath.startsWith("http")?new URL(filePath).openConnection().getInputStream():filePath.contains(".xls")?new FileInputStream(new File(Utils.revealLocalFile(filePath))):new ByteArrayInputStream(Utils.base64Decode(filePath));
        Workbook workbook = WorkbookFactory.create(input, password!=""?password:null);
        JsonArrayBuilder columnList = Json.createArrayBuilder();
        JsonArrayBuilder dataList = Json.createArrayBuilder();
        try {
            Sheet sheet = workbook.getSheetAt(sheetNumber-1);

            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            
            boolean seriesOnColumns = !isNumber(cellSeries.charAt(0));
            
            ArrayList<Integer> cellValuesIndexList = new ArrayList<Integer>();
            for(String cellValue : cellValues.split(",")){
                if(cellValue.contains("-")){
                    String[] cellValueRangeSplit = cellValue.split("-");
                    if(cellValueRangeSplit.length != 2) throw new Exception("Excel cell value format incorrect: "+cellValue);
                    if(cellValueRangeSplit[0].isEmpty()) throw new Exception("Excel cell value range start not provided");
                    if(cellValueRangeSplit[1].isEmpty()) throw new Exception("Excel cell value range end not provided");
                    int cellValueStart = seriesOnColumns?Integer.parseInt(cellValueRangeSplit[0])-1:convertColumnToNumber(cellValueRangeSplit[0])-1;
                    int cellValueEnd = seriesOnColumns?Integer.parseInt(cellValueRangeSplit[1])-1:convertColumnToNumber(cellValueRangeSplit[1])-1;
                    boolean ascendentOrder = cellValueStart<=cellValueEnd;
                    for(int i=cellValueStart;(ascendentOrder)?i<=cellValueEnd:i>=cellValueEnd;i=(ascendentOrder)?i+1:i-1)
                        cellValuesIndexList.add(i);
                } else {
                    if(cellValue.isEmpty()) throw new Exception("Excel cell value not provided");
                    cellValuesIndexList.add(seriesOnColumns?Integer.parseInt(cellValue)-1:convertColumnToNumber(cellValue)-1);
                }
            }
            
            ArrayList<String> cellSeriesNameList = new ArrayList<String>();
            ArrayList<Integer> cellSeriesIndexList = new ArrayList<Integer>();
            for(String cellSerie : cellSeries.split(",")){
                if(cellSerie.contains("-")){
                    String[] cellSerieRangeSplit = cellSerie.split("-");
                    if(cellSerieRangeSplit.length != 2) throw new Exception("Excel cell serie format incorrect: "+cellSerie);
                    if(cellSerieRangeSplit[0].isEmpty()) throw new Exception("Excel cell serie range start not provided");
                    if(cellSerieRangeSplit[1].isEmpty()) throw new Exception("Excel cell serie range end not provided");
                    int cellSerieStart = !seriesOnColumns?Integer.parseInt(cellSerieRangeSplit[0])-1:convertColumnToNumber(cellSerieRangeSplit[0])-1;
                    int cellSerieEnd = !seriesOnColumns?Integer.parseInt(cellSerieRangeSplit[1])-1:convertColumnToNumber(cellSerieRangeSplit[1])-1;
                    boolean ascendentOrder = cellSerieStart<=cellSerieEnd;
                    for(int i=cellSerieStart;(ascendentOrder)?i<=cellSerieEnd:i>=cellSerieEnd;i=(ascendentOrder)?i+1:i-1) {
                        cellSeriesIndexList.add(i);
                        //cellSeriesNameList.add(seriesOnColumns?"Column "+convertNumberToColumn(i+1):"Row "+(i+1));
                        cellSeriesNameList.add(seriesOnColumns?""+convertNumberToColumn(i+1):""+(i+1));
                    }
                } else {
                    if(cellSerie.isEmpty()) throw new Exception("Excel cell serie not provided");
                    cellSeriesIndexList.add(!seriesOnColumns?Integer.parseInt(cellSerie)-1:convertColumnToNumber(cellSerie)-1);
                    //cellSeriesNameList.add(seriesOnColumns?"Column "+cellSerie:"Row "+cellSerie);
                    cellSeriesNameList.add(cellSerie);
                }
            }
            
            if(!cellSeriesName.isEmpty()) { 
                if(cellSeriesName.charAt(0) == '[') {
                    JsonArray jsonArray = Json.createReader(new StringReader(cellSeriesName)).readArray();
                    if(jsonArray.size() != cellSeriesIndexList.size()) throw new Exception("You request " + cellSeriesIndexList.size() + " series but provided a name for " + jsonArray.size() + ". Number of series and names must match.");
                    for(int i=0; i<jsonArray.size(); i++)
                        cellSeriesNameList.set(i, jsonArray.getString(i));
                } else {
                    int seriesNameIndex = seriesOnColumns?Integer.parseInt(cellSeriesName)-1:convertColumnToNumber(cellSeriesName)-1;
                    for(int i=0; i<cellSeriesIndexList.size(); i++) {
                        Row row = sheet.getRow(seriesOnColumns?seriesNameIndex:cellSeriesIndexList.get(i));
                        Cell cell = row==null?null:row.getCell(seriesOnColumns?cellSeriesIndexList.get(i):seriesNameIndex);
                        cellSeriesNameList.set(i, dataFormatter.formatCellValue(cell, evaluator));
                    }
                }
            }
            
            for(int i=0; i<cellValuesIndexList.size(); i++) {
                JsonObjectBuilder data = Json.createObjectBuilder();
                for(int j=0; j<cellSeriesIndexList.size(); j++) {
                    Row row = sheet.getRow(seriesOnColumns?cellValuesIndexList.get(i):cellSeriesIndexList.get(j));
                    Cell cell = row==null?null:row.getCell(seriesOnColumns?cellSeriesIndexList.get(j):cellValuesIndexList.get(i));
                    data.add(cellSeriesNameList.get(j), dataFormatter.formatCellValue(cell, evaluator));
                }
                dataList.add(data);
            }
            
            for(int i=0; i<cellSeriesNameList.size(); i++)
                columnList.add(cellSeriesNameList.get(i));
        } finally {
            workbook.close();
            //input.close();
        }
        return Json.createObjectBuilder()
            .add("columns", columnList)
            .add("data", dataList)
            .add("moreInfo", Json.createObjectBuilder().add("retrievalTime", Utils.getCurrentTime()))
            .build();
    }
    
    @Override
    public void stop() throws Exception {
        filePath = "";
        password = "";
        //if(input!=null)
        //    input.close();
        //input = null;
        //workbook = null;
    }
    
    private boolean isNumber(char character){
        return character >= '0' && character <= '9';
    }
    
    public static String convertNumberToColumn(int number) {
        final StringBuilder sb = new StringBuilder();
        int num = number - 1;
        while (num >=  0) {
            int numChar = (num % 26) + 65;
            sb.append((char)numChar);
            num = (num / 26) - 1;
        }
        return sb.reverse().toString();
    }
    
    public static int convertColumnToNumber(String columnUpperCase){
        int sum = 0;
        for (int i = 0; i < columnUpperCase.length(); i++)
            sum = (sum * 26) + (columnUpperCase.charAt(i) - 'A' + 1);
        return sum;
    }
    
    /*
    public static void main(String[] argv) throws Exception{
        ExcelConnector connector = new ExcelConnector();
        Utils.uploadFolder = "";
        try{
            connector.threadStart(Json.createObjectBuilder()
                .add("filePath", Json.createObjectBuilder().add("value", "D:\\ADOXX.ORG\\DASHBOARD\\gitlab\\kpi-dashboard\\src\\main\\resources\\dsExcelTest.xlsx"))
                .add("password", Json.createObjectBuilder().add("value", ""))

                .build()
            );
            connector.waitThreadStart();
            JsonObject callOutputJson = connector.performCallSafe(Json.createObjectBuilder()
                .add("sheetNumber", Json.createObjectBuilder().add("value", "1"))
                .add("cellSeries", Json.createObjectBuilder().add("value", "B,C")) //7,8-10 //B,E-C
                .add("cellSeriesName", Json.createObjectBuilder().add("value", "")) //B //2
                .add("cellValues", Json.createObjectBuilder().add("value", "3,4")) //C,D //4-3
                .build()
            );
            System.out.println(callOutputJson);
        } finally {connector.threadStop();}
    }
    */
}
