/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.dspace.app.migration.MigrateScript;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class MigrateConnectionManager {
    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
        .getConfigurationService();

    private MigrateConnectionManager() {

    }

    public static void getAllProperties() throws SQLException {
        getProperties("rp_prop.sql", MigrateScript.RP_CSV);
        getProperties("pj_prop.sql", MigrateScript.PJ_CSV);
        getProperties("ou_prop.sql", MigrateScript.OU_CSV);
        getProperties("do_prop.sql", MigrateScript.DO_CSV);
        System.out.println("Loaded CRIS prop");
    }

    public static String[] resolvePointer(String pointerType, long pointerId) throws SQLException {
        Connection connection = DatabaseUtils.getDataSource().getConnection();
        PreparedStatement stmt = connection
            .prepareStatement(sqlReader("get_pointer.sql"));
        stmt.setString(1, pointerType);
        stmt.setLong(2, pointerId);
        // RP
        ResultSet rs = stmt.executeQuery();
        String[] result = new String[2];
        int i = 0;
        while (rs.next()) {
            i++;
            result[0] = rs.getString("crisid");
            result[1] = rs.getString("metadata_value");
        }

        if (i == 0) {
            //  directly get crisid if it has no no name
            stmt = connection
                    .prepareStatement(sqlReader("get_pointer_noname.sql"));
            stmt.setString(1, pointerType);
            stmt.setLong(2, pointerId);
            rs = stmt.executeQuery();
            result = new String[2];
            while (rs.next()) {
                i++;
                result[0] = rs.getString("crisid");
                result[1] = result[0];
            }
        }

        rs.close();
        stmt.close();
        connection.close();
        return result;
    }

    public static String[] resolveEpersonPointer(long pointerId) throws SQLException {
        Connection connection = DatabaseUtils.getDataSource().getConnection();
        PreparedStatement stmt = connection
            .prepareStatement(sqlReader("get_eperson_pointer.sql"));
        stmt.setLong(1, pointerId);
        // RP
        ResultSet rs = stmt.executeQuery();
        String[] result = new String[2];
        while (rs.next()) {
            result[0] = rs.getString("uuid");
            result[1] = rs.getString("email");
        }

        rs.close();
        stmt.close();
        connection.close();
        return result;
    }

    public static void getProperties(String sqlFile, String outputFile) throws SQLException {
        Connection connection = DatabaseUtils.getDataSource().getConnection();
        Statement stmt = connection.createStatement();
        // RP
        ResultSet rs = stmt.executeQuery(sqlReader(sqlFile));
        try (CSVPrinter printer = new CSVPrinter(
            new FileWriter(
                configurationService.getProperty("dspace.dir") + outputFile),
            CSVFormat.EXCEL)) {
            printer.printRecord("crisid", "shortname", "parent_id", "positiondef",
                "nested_object_id", "visibility", "textvalue", "datevalue",
                "dtype", "rpvalue", "projectvalue", "ouvalue", "dovalue",
                "filefolder", "filename", "fileextension", "booleanvalue", "linkdescription",
                "linkvalue", "doublevalue", "classificationvalue", "custompointer", "sourceid", "sourceref");
            while (rs.next()) {
                printer.printRecord(rs.getString("crisid"), rs.getString("shortname"), rs.getLong("parent_id"),
                    rs.getLong("positiondef"),
                    rs.getLong("nested_object_id"), rs.getLong("visibility"), rs.getString("textvalue"),
                    rs.getDate("datevalue"), rs.getString("dtype"), rs.getLong("rpvalue"),
                    rs.getLong("projectvalue"), rs.getLong("ouvalue"), rs.getLong("dovalue"),
                    rs.getString("filefolder"), rs.getString("filename"), rs.getString("fileextension"),
                    rs.getBoolean("booleanvalue"),
                    rs.getString("linkdescription"), rs.getString("linkvalue"), rs.getDouble("doublevalue"),
                    rs.getLong("classificationvalue"), rs.getLong("custompointer"),
                    rs.getString("sourceid"), rs.getString("sourceref"));

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        rs.close();
        stmt.close();
        connection.close();
    }

    public static String sqlReader(String fileName) {
        String basePath = configurationService.getProperty("dspace.dir") + "/config/migration/sql/";
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(basePath + fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n"); // Append each line to the StringBuilder
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileContent = text.toString();
        return fileContent;
    }
}
