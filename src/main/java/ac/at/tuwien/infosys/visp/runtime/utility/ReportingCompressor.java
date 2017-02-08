package ac.at.tuwien.infosys.visp.runtime.utility;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportingCompressor {

    private static final String SOURCE_FOLDER = "reporting";
    private List<String> fileList;

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public void zipIt() {
        File node = new File("reporting");
        fileList = new ArrayList<>();
        generateFileList(node);

        byte[] buffer = new byte[1024];
        String source = "";
        ZipOutputStream zos = null;
        try {
            try {
                source = SOURCE_FOLDER.substring(SOURCE_FOLDER.lastIndexOf("\\") + 1, SOURCE_FOLDER.length());
            } catch (Exception e) {
                source = SOURCE_FOLDER;
            }

            zos = new ZipOutputStream(new FileOutputStream("reporting/" + new DateTime(DateTimeZone.UTC) + ".zip"));

            FileInputStream in = null;

            for (String file : fileList) {
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(SOURCE_FOLDER + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    in.close();
                }
            }

            zos.closeEntry();

        } catch (IOException e) {
            LOG.error(e.getMessage());
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    public void generateFileList(File node) {
        if (node.isFile()) {
            fileList.add(node.toString().substring(SOURCE_FOLDER.length() + 1, node.toString().length()));
        }

        if (node.isDirectory()) {
            for (File file : node.listFiles((dir, filename) -> filename.endsWith(".csv"))) {
                generateFileList(new File(node, file.getName()));
            }
        }
    }

    public void cleanup() {
        File node = new File("reporting");

        fileList = new ArrayList<>();
        generateFileList(node);

        for (String file : fileList) {
            File f = new File(SOURCE_FOLDER + File.separator + file);
            f.delete();
        }

    }

}
