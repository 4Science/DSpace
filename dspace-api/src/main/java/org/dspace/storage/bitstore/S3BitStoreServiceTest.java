/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.math.NumberUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

public class S3BitStoreServiceTest {

    /** options */
    private static final Option TOTAL_OPT = OptionBuilder.hasArg(true)
            .withLongOpt("total")
            .withDescription("total number of files")
            .withType(Number.class)
            .create("t");

    private static final Option RPP_OPT = OptionBuilder.hasArg(true)
            .withLongOpt("rpp")
            .withDescription("number of files for each thread")
            .withType(Number.class)
            .create("r");

    private static final Option ITEM_HANDLE_OPT = OptionBuilder.hasArg(true)
            .withLongOpt("handle")
            .withDescription("item handle")
            .create("i");

    private static final Option FILE_OPT = OptionBuilder.hasArg(true)
            .withLongOpt("file")
            .withDescription("file to import")
            .create("f");

    public static void main(String[] args) throws ParseException {
        // read parameters
        Options options = new Options();
        options.addOption(TOTAL_OPT);
        options.addOption(RPP_OPT);
        options.addOption(ITEM_HANDLE_OPT);
        options.addOption(FILE_OPT);
        CommandLine line = new PosixParser().parse(options, args);
        int tot = NumberUtils.toInt(line.getOptionValue("t"));
        int rpp = NumberUtils.toInt(line.getOptionValue("r"));
        String handle = line.getOptionValue("i");
        String path = line.getOptionValue("f");

        File file;
        Context context = null;
        try {
            file = new File(path);

            // init objects
            context = new Context();
            context.turnOffAuthorisationSystem();
            DSpaceObject dso = HandleManager.resolveToObject(context, handle);
            Item item = null;
            if (dso != null && dso instanceof Item) {
                item = (Item)dso;
            }

            // create bitstream
            S3BitStoreServiceTest s3BitStoreServiceTest = new S3BitStoreServiceTest();
            s3BitStoreServiceTest.startMultiThread(item, file, tot, rpp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    private void startMultiThread(Item item, File file, int tot, int rpp) {
        List<ProcessBitstreamsThread> threads = new ArrayList<ProcessBitstreamsThread>();
        for (int i = 0; i < tot/rpp; i++) {
            ProcessBitstreamsThread thread = new ProcessBitstreamsThread(item, file, i*rpp, (i*rpp)+rpp);
            thread.start();
            threads.add(thread);
        }
        boolean finished = false;
        while (!finished) {
            finished = true;
            for (ProcessBitstreamsThread thread : threads) {
                finished = finished && !thread.isAlive();
            }
        }
    }

    class ProcessBitstreamsThread extends Thread {
        private Item item;
        private File file;
        private int i;
        private int n;

        public ProcessBitstreamsThread(Item item, File file, int i, int n) {
            this.item = item;
            this.file = file;
            this.i = i;
            this.n = n;
        }

        @Override
        public void run() {
            String key = "";
            try {
                for (int j = i; j < n; j++) {
                    key = file + " " + j;
                    System.out.println("processing " + key);
	                item.createSingleBitstream(new FileInputStream(file));
                }
            } catch (AuthorizeException | IOException | SQLException e) {
                System.out.println("\n\n\n\n" + "error on object: " + key);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
