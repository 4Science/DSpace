/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the rdf storage impl suitable to generate a local
 * .ttl file of the requested content to be loaded in fuseki using external
 * tools (as the tbd2.tbdloader provided by jena). Delete/update is not
 * supported the file is cleaned/generated at each execution
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 */
public class RDFFileStorageImpl extends RDFStorageImpl {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RDFFileStorageImpl.class);

    private OutputStream out;

    private ConfigurationService configurationService;

    @Autowired(required = true)
    public RDFFileStorageImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        String fileStorage = configurationService.getProperty("rdf.filestorage.location");
        try {
            File file = new File(fileStorage);
            Path path = Paths.get(file.getParent());
            Files.createDirectories(path);
            out = new FileOutputStream(file, true);
        } catch (IOException e) {
            log.error("Invalid file storage location", e);
        }
    }

    @Override
    public void store(String uri, Model model) {
        try {
            Dataset dataset = DatasetFactory.create();
            dataset.addNamedModel(uri, model);
            RDFDataMgr.write(out, dataset, Lang.NQUADS);
            out.flush();
        } catch (IOException e) {
            log.error("Fail to store the model ", e);
        }
    }

    /**
     * Used as destroy method in the spring bean configuration
     * @throws Throwable
     */
    public void destroy() throws Throwable {
        out.flush();
        out.close();
    }

    @Override
    public void delete(String uri) {
        log.error("RDFFileStorageImpl#delete not implemented");
        throw new RuntimeException("RDFFileStorageImpl#delete not implemented");
    }

    @Override
    public void deleteAll() {
        try {
            String fileStorage = configurationService.getProperty("rdf.filestorage.location");
            out.close();
            File file = new File(fileStorage);
            if (file.exists()) {
                file.delete();
            }
            Path path = Paths.get(file.getParent());
            Files.createDirectories(path);
            file = new File(fileStorage);
            out = new FileOutputStream(file);
        } catch (IOException e) {
            log.error("Invalid file storage location", e);
        }
    }

    @Override
    public List<String> getAllStoredGraphs() {
        log.error("RDFFileStorageImpl#getAllStoredGraphs not implemented");
        throw new RuntimeException("RDFFileStorageImpl#getAllStoredGraphs not implemented");
    }

}
